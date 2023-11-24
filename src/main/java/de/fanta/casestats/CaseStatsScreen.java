package de.fanta.casestats;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.StatsListener;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class CaseStatsScreen extends Screen implements StatsListener {
    static final Identifier SLOT_TEXTURE = new Identifier("container/slot");
    static final Identifier HEADER_TEXTURE = new Identifier("statistics/header");
    static final Identifier SORT_UP_TEXTURE = new Identifier("statistics/sort_up");
    static final Identifier SORT_DOWN_TEXTURE = new Identifier("statistics/sort_down");
    private static final Text DOWNLOADING_STATS_TEXT = Text.translatable("multiplayer.downloadingStats");
    static final Text NONE_TEXT = Text.translatable("stats.none");
    protected final Screen parent;
    private CaseStatsListWidget caseStats;
    final StatHandler statHandler;

    @Nullable
    private AlwaysSelectedEntryListWidget<?> selectedList;
    private boolean downloadingStats = true;

    public CaseStatsScreen(Screen parent, StatHandler statHandler) {
        super(Text.translatable("gui.stats"));
        this.parent = parent;
        this.statHandler = statHandler;
    }

    protected void init() {
        // Request data //TODO: Connect to global server -> sync local changes -> fetch remote data :Access Key required:
        this.downloadingStats = true;
    }

    public void createLists() {
        this.caseStats = new CaseStatsListWidget(this.client);
    }

    public void createButtons() {
        ButtonWidget buttonWidget = this.addDrawableChild(
                ButtonWidget.builder(Text.translatable("stat.itemsButton"),
                (button) -> this.selectStatList(this.caseStats))
                        .dimensions(this.width / 2 - 40, this.height - 52, 80, 20).build()
        );
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE,
                (button) -> this.client.setScreen(this.parent))
                        .dimensions(this.width / 2 - 100, this.height - 28, 200, 20).build()
        );
        if (this.caseStats.children().isEmpty()) {
            buttonWidget.active = false;
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.downloadingStats) {
            this.renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, DOWNLOADING_STATS_TEXT, this.width / 2, this.height / 2, 16777215);
            TextRenderer var10001 = this.textRenderer;
            String var10002 = PROGRESS_BAR_STAGES[(int) (Util.getMeasuringTimeMs() / 150L % (long) PROGRESS_BAR_STAGES.length)];
            int var10003 = this.width / 2;
            int var10004 = this.height / 2;
            Objects.requireNonNull(this.textRenderer);
            context.drawCenteredTextWithShadow(var10001, var10002, var10003, var10004 + 9 * 2, 16777215);
        } else {
            super.render(context, mouseX, mouseY, delta);
            this.getSelectedStatList().render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 16777215);
        }
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(context);
    }

    public void onStatsReady() {
        if (this.downloadingStats) {
            this.createLists();
            this.createButtons();
            this.selectStatList(this.caseStats);
            this.downloadingStats = false;
        }

    }

    public boolean shouldPause() {
        return !this.downloadingStats;
    }

    @Nullable
    public AlwaysSelectedEntryListWidget<?> getSelectedStatList() {
        return this.selectedList;
    }

    public void selectStatList(@Nullable AlwaysSelectedEntryListWidget<?> list) {
        if (this.selectedList != null) {
            this.remove(this.selectedList);
        }

        if (list != null) {
            this.addSelectableChild(list);
            this.selectedList = list;
        }

    }

    static String getStatTranslationKey(Stat<Identifier> stat) {
        String var10000 = stat.getValue().toString();
        return "stat." + var10000.replace(':', '.');
    }

    int getColumnX(int index) {
        return 115 + 40 * index;
    }

    void renderStatItem(DrawContext context, int x, int y, Item item) {
        this.renderIcon(context, x + 1, y + 1, SLOT_TEXTURE);
        context.drawItemWithoutEntity(item.getDefaultStack(), x + 2, y + 2);
    }

    void renderIcon(DrawContext context, int x, int y, Identifier texture) {
        context.drawGuiTexture(texture, x, y, 0, 18, 18);
    }

    @Environment(EnvType.CLIENT)
    private class CaseStatsListWidget extends AlwaysSelectedEntryListWidget<CaseStatsListWidget.Entry> {
        protected final List<StatType<Block>> blockStatTypes = Lists.newArrayList();
        protected final List<StatType<Item>> itemStatTypes;
        private final Identifier[] headerIconTextures = new Identifier[]{new Identifier("statistics/block_mined"), new Identifier("statistics/item_broken"), new Identifier("statistics/item_crafted"), new Identifier("statistics/item_used"), new Identifier("statistics/item_picked_up"), new Identifier("statistics/item_dropped")};
        protected int selectedHeaderColumn = -1;
        protected final ItemComparator comparator = new ItemComparator();
        @Nullable
        protected StatType<?> selectedStatType;
        protected int listOrder;

        public CaseStatsListWidget(MinecraftClient client) {
            super(client, CaseStatsScreen.this.width, CaseStatsScreen.this.height, 32, CaseStatsScreen.this.height - 64, 20);
            this.blockStatTypes.add(Stats.MINED);
            this.itemStatTypes = Lists.newArrayList(Stats.BROKEN, Stats.CRAFTED, Stats.USED, Stats.PICKED_UP, Stats.DROPPED);
            this.setRenderHeader(true, 20);
            Set<Item> set = Sets.newIdentityHashSet();
            Iterator var4 = Registries.ITEM.iterator();

            Item item;
            boolean bl;
            Iterator var7;
            StatType statType;
            while (var4.hasNext()) {
                item = (Item) var4.next();
                bl = false;
                var7 = this.itemStatTypes.iterator();

                while (var7.hasNext()) {
                    statType = (StatType) var7.next();
                    if (statType.hasStat(item) && CaseStatsScreen.this.statHandler.getStat(statType.getOrCreateStat(item)) > 0) {
                        bl = true;
                    }
                }

                if (bl) {
                    set.add(item);
                }
            }

            var4 = Registries.BLOCK.iterator();

            while (var4.hasNext()) {
                Block block = (Block) var4.next();
                bl = false;
                var7 = this.blockStatTypes.iterator();

                while (var7.hasNext()) {
                    statType = (StatType) var7.next();
                    if (statType.hasStat(block) && CaseStatsScreen.this.statHandler.getStat(statType.getOrCreateStat(block)) > 0) {
                        bl = true;
                    }
                }

                if (bl) {
                    set.add(block.asItem());
                }
            }

            set.remove(Items.AIR);
            var4 = set.iterator();

            while (var4.hasNext()) {
                item = (Item) var4.next();
                this.addEntry(new Entry(item));
            }
        }

        protected void renderHeader(DrawContext context, int x, int y) {
            if (!this.client.mouse.wasLeftButtonClicked()) {
                this.selectedHeaderColumn = -1;
            }

            int i;
            Identifier identifier;
            for (i = 0; i < this.headerIconTextures.length; ++i) {
                identifier = this.selectedHeaderColumn == i ? CaseStatsScreen.SLOT_TEXTURE : CaseStatsScreen.HEADER_TEXTURE;
                CaseStatsScreen.this.renderIcon(context, x + CaseStatsScreen.this.getColumnX(i) - 18, y + 1, identifier);
            }

            if (this.selectedStatType != null) {
                i = CaseStatsScreen.this.getColumnX(this.getHeaderIndex(this.selectedStatType)) - 36;
                identifier = this.listOrder == 1 ? CaseStatsScreen.SORT_UP_TEXTURE : CaseStatsScreen.SORT_DOWN_TEXTURE;
                CaseStatsScreen.this.renderIcon(context, x + i, y + 1, identifier);
            }

            for (i = 0; i < this.headerIconTextures.length; ++i) {
                int j = this.selectedHeaderColumn == i ? 1 : 0;
                CaseStatsScreen.this.renderIcon(context, x + CaseStatsScreen.this.getColumnX(i) - 18 + j, y + 1 + j, this.headerIconTextures[i]);
            }

        }

        public int getRowWidth() {
            return 375;
        }

        protected int getScrollbarPositionX() {
            return this.width / 2 + 140;
        }

        protected void clickedHeader(int x, int y) {
            this.selectedHeaderColumn = -1;

            for (int i = 0; i < this.headerIconTextures.length; ++i) {
                int j = x - CaseStatsScreen.this.getColumnX(i);
                if (j >= -36 && j <= 0) {
                    this.selectedHeaderColumn = i;
                    break;
                }
            }

            if (this.selectedHeaderColumn >= 0) {
                this.selectStatType(this.getStatType(this.selectedHeaderColumn));
                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }

        }

        private StatType<?> getStatType(int headerColumn) {
            return headerColumn < this.blockStatTypes.size() ? (StatType) this.blockStatTypes.get(headerColumn) : (StatType) this.itemStatTypes.get(headerColumn - this.blockStatTypes.size());
        }

        private int getHeaderIndex(StatType<?> statType) {
            int i = this.blockStatTypes.indexOf(statType);
            if (i >= 0) {
                return i;
            } else {
                int j = this.itemStatTypes.indexOf(statType);
                return j >= 0 ? j + this.blockStatTypes.size() : -1;
            }
        }

        protected void renderDecorations(DrawContext context, int mouseX, int mouseY) {
            if (mouseY >= this.top && mouseY <= this.bottom) {
                CaseStatsListWidget.Entry entry = (CaseStatsScreen.CaseStatsListWidget.Entry) this.getHoveredEntry();
                int i = (this.width - this.getRowWidth()) / 2;
                if (entry != null) {
                    if (mouseX < i + 40 || mouseX > i + 40 + 20) {
                        return;
                    }

                    Item item = entry.getItem();
                    context.drawTooltip(CaseStatsScreen.this.textRenderer, this.getText(item), mouseX, mouseY);
                } else {
                    Text text = null;
                    int j = mouseX - i;

                    for (int k = 0; k < this.headerIconTextures.length; ++k) {
                        int l = CaseStatsScreen.this.getColumnX(k);
                        if (j >= l - 18 && j <= l) {
                            text = this.getStatType(k).getName();
                            break;
                        }
                    }

                    if (text != null) {
                        context.drawTooltip(CaseStatsScreen.this.textRenderer, text, mouseX, mouseY);
                    }
                }

            }
        }

        protected Text getText(Item item) {
            return item.getName();
        }

        protected void selectStatType(StatType<?> statType) {
            if (statType != this.selectedStatType) {
                this.selectedStatType = statType;
                this.listOrder = -1;
            } else if (this.listOrder == -1) {
                this.listOrder = 1;
            } else {
                this.selectedStatType = null;
                this.listOrder = 0;
            }

            this.children().sort(this.comparator);
        }

        @Environment(EnvType.CLIENT)
        private class ItemComparator implements Comparator<CaseStatsListWidget.Entry> {
            ItemComparator() {
            }

            public int compare(CaseStatsListWidget.Entry entry, CaseStatsListWidget.Entry entry2) {
                Item item = entry.getItem();
                Item item2 = entry2.getItem();
                int i;
                int j;
                if (CaseStatsListWidget.this.selectedStatType == null) {
                    i = 0;
                    j = 0;
                } else {
                    StatType statType;
                    if (CaseStatsListWidget.this.blockStatTypes.contains(CaseStatsListWidget.this.selectedStatType)) {
                        statType = CaseStatsListWidget.this.selectedStatType;
                        i = item instanceof BlockItem ? CaseStatsScreen.this.statHandler.getStat(statType, ((BlockItem) item).getBlock()) : -1;
                        j = item2 instanceof BlockItem ? CaseStatsScreen.this.statHandler.getStat(statType, ((BlockItem) item2).getBlock()) : -1;
                    } else {
                        statType = CaseStatsListWidget.this.selectedStatType;
                        i = CaseStatsScreen.this.statHandler.getStat(statType, item);
                        j = CaseStatsScreen.this.statHandler.getStat(statType, item2);
                    }
                }

                return i == j ? CaseStatsListWidget.this.listOrder * Integer.compare(Item.getRawId(item), Item.getRawId(item2)) : CaseStatsListWidget.this.listOrder * Integer.compare(i, j);
            }
        }

        @Environment(EnvType.CLIENT)
        private class Entry extends AlwaysSelectedEntryListWidget.Entry<CaseStatsListWidget.Entry> {
            private final Item item;

            Entry(Item item) {
                this.item = item;
            }

            public Item getItem() {
                return this.item;
            }

            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                CaseStatsScreen.this.renderStatItem(context, x + 40, y, this.item);

                int i;
                for (i = 0; i < CaseStatsScreen.this.caseStats.blockStatTypes.size(); ++i) {
                    Stat stat = ((StatType) CaseStatsScreen.this.caseStats.blockStatTypes.get(i)).getOrCreateStat(((BlockItem) this.item).getBlock());

                    this.render(context, stat, x + CaseStatsScreen.this.getColumnX(i), y, index % 2 == 0);
                }

                for (i = 0; i < CaseStatsScreen.this.caseStats.itemStatTypes.size(); ++i) {
                    this.render(context, ((StatType) CaseStatsScreen.this.caseStats.itemStatTypes.get(i)).getOrCreateStat(this.item), x + CaseStatsScreen.this.getColumnX(i + CaseStatsScreen.this.caseStats.blockStatTypes.size()), y, index % 2 == 0);
                }

            }

            protected void render(DrawContext context, @Nullable Stat<?> stat, int x, int y, boolean white) {
                Text text = stat == null ? CaseStatsScreen.NONE_TEXT : Text.literal(stat.format(CaseStatsScreen.this.statHandler.getStat(stat)));
                context.drawTextWithShadow(CaseStatsScreen.this.textRenderer, (Text) text, x - CaseStatsScreen.this.textRenderer.getWidth((StringVisitable) text), y + 5, white ? 16777215 : 9474192);
            }

            public Text getNarration() {
                return Text.translatable("narrator.select", new Object[]{this.item.getName()});
            }
        }
    }
}
