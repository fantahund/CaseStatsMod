package de.fanta.casestats.screens;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import de.fanta.casestats.CaseStats;
import de.fanta.casestats.CaseStatsGlobalDataRequestType;
import de.fanta.casestats.config.ConfigGui;
import de.fanta.casestats.config.Configs;
import de.fanta.casestats.data.CaseItem;
import de.fanta.casestats.data.CaseStat;
import de.fanta.casestats.data.PlayerCaseItemStat;
import de.fanta.casestats.data.Stats;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.minecraft.client.gui.screen.StatsListener.PROGRESS_BAR_STAGES;

@Environment(EnvType.CLIENT)
public class CaseStatsScreen extends Screen {
    static final Identifier SLOT_TEXTURE = new Identifier("container/slot");
    static final Identifier HEADER_TEXTURE = new Identifier("statistics/header");
    static final Identifier SORT_UP_TEXTURE = new Identifier("statistics/sort_up");
    static final Identifier SORT_DOWN_TEXTURE = new Identifier("statistics/sort_down");
    private static final Text DOWNLOADING_STATS_TEXT = Text.translatable("multiplayer.downloadingStats");
    static final Text NONE_TEXT = Text.translatable("stats.none");
    private static final Map<UUID, GameProfile> CACHED_USER_PROFILES = new HashMap<>();

    protected final Screen parent;
    private CaseStatsListWidget caseStats;
    private final CaseStats caseStatsMod;
    private Stats cachedStats;

    @Nullable
    private AlwaysSelectedEntryListWidget<?> selectedList;
    private boolean downloadingStats = true;

    public CaseStatsScreen(Screen parent) {
        super(Text.literal("Case-Statistics"));
        this.parent = parent;
        this.caseStatsMod = CaseStats.getInstance();
    }

    @Override
    protected void init() {
        this.downloadingStats = true;
        this.cachedStats = new Stats();

        Future<List<CaseStat>> statsFuture = caseStatsMod.getGlobalDataRequestManager().makeRequest(CaseStatsGlobalDataRequestType.GET_CASES, caseStatsMod.getConnectionAPI().getServer("casestatsserver"), new Object[0]);
        Thread thread = new Thread(() -> {
            List<CaseStat> stats;
            try {
                stats = statsFuture.get(10, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
            for (CaseStat stat : stats) {
                cachedStats.add(stat);
            }
            onStatsReady();
        });
        thread.start();

//        this.createLists();
//        this.createButtons();
//        this.selectStatList(this.caseStats);
    }

    public void createLists() {
        this.caseStats = new CaseStatsListWidget(this.client);
    }

    public void createButtons() {
        int i = 0;
        for (CaseStat caseStat : cachedStats.caseStats()) {
            this.addDrawableChild(new ItemStackButtonWidget(this.width / 2 - i * 22, this.height - 52, 20, 20, caseStat.icon(), button -> {
                if (caseStats != null && caseStat != caseStats.selectedCase) {
                    fetchCaseStats(caseStat);
                }
            }));
            i++;
        }
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE,
                        (button) -> this.client.setScreen(this.parent))
                .dimensions(this.width / 2 - 100, this.height - 28, 100, 20).build()
        );

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Settings"),
                        (button) -> this.client.setScreen(new ConfigGui()))
                .dimensions(this.width / 2 - 0, this.height - 28, 100, 20).build()
        );
    }

    private void fetchCaseStats(CaseStat caseStat) {
        downloadingStats = true;
        List<String> names = Configs.Generic.StatsPlayer.getStrings();
        Object[] data = new Object[names.size() + 2];
        data[0] = caseStat.id();
        data[1] = names.size();
        int index = 2;
        for (String name : names) {
            data[index] = name;
            index++;
        }

        Future<List<PlayerCaseItemStat>> statsFuture = caseStatsMod.getGlobalDataRequestManager().makeRequest(CaseStatsGlobalDataRequestType.GET_CASE_STATS, caseStatsMod.getConnectionAPI().getServer("casestatsserver"), data);
        Thread thread = new Thread(() -> {
            List<PlayerCaseItemStat> stats;
            try {
                stats = statsFuture.get(10, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                onStatsReady();
                CaseStats.LOGGER.log(Level.ERROR, "", e);
                return;
            }
            for (PlayerCaseItemStat stat : stats) {
                caseStat.setItemOccurrence(stat.player(), stat.caseItem(), stat.count());
            }

            if (downloadingStats) {
                caseStats.setSelectedCaseStat(caseStat);
                downloadingStats = false;
            }
        });
        thread.start();
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
            if (caseStats.selectedCase != null) {
                fetchCaseStats(caseStats.selectedCase);
            } else {
                this.downloadingStats = false;
            }
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

    int getColumnX(int index) {
        return 115 + 40 * index;
    }

    void renderStatItem(DrawContext context, int x, int y, ItemStack item) {
        this.renderIcon(context, x + 1, y + 1, SLOT_TEXTURE);
        context.drawItem(item, x + 2, y + 2);
        if (item.getCount() > 1) {
            String string = String.valueOf(item.getCount());
            context.getMatrices().translate(0.0F, 0.0F, 200.0F);
            context.drawText(textRenderer, string, x + 19 - 1 - textRenderer.getWidth(string), y + 6 + 4, 16777215, true);
        }
    }

    void renderIcon(DrawContext context, int x, int y, Identifier texture) {
        context.drawGuiTexture(texture, x, y, 0, 18, 18);
    }

    private static Optional<GameProfile> fetchProfile(UUID uuid) {
        if (CACHED_USER_PROFILES.containsKey(uuid)) return Optional.ofNullable(CACHED_USER_PROFILES.get(uuid));


        MinecraftSessionService service = MinecraftClient.getInstance().getSessionService();
        ProfileResult result = service.fetchProfile(uuid, false);
        if (result != null) {
            CACHED_USER_PROFILES.put(uuid, result.profile());
            return Optional.of(result.profile());
        }
        CACHED_USER_PROFILES.put(uuid, null);
        return Optional.empty();
    }

    @Environment(EnvType.CLIENT)
    private class CaseStatsListWidget extends AlwaysSelectedEntryListWidget<CaseStatsListWidget.Entry> {
        protected int selectedHeaderColumn = -1;
        protected CaseStat selectedCase = null;
        protected final ItemComparator comparator = new ItemComparator();
        protected static int customHeaderHeight = 20;
        protected int listOrder;

        public CaseStatsListWidget(MinecraftClient client) {
            super(client, CaseStatsScreen.this.width, CaseStatsScreen.this.height - customHeaderHeight - 108, 56, 20);
            setRenderHeader(false, 0);
            selectedCase = cachedStats.caseStats().stream().findFirst().orElse(null);
        }

        public void setSelectedCaseStat(CaseStat caseStat) {
            clearEntries();
            if (caseStat == null) return;
            this.selectedCase = caseStat;

            for (CaseItem caseItem : selectedCase.totals().keySet()) {
                this.addEntry(new Entry(caseItem));
            }

            Collection<CaseStat.PlayerStat> playerStats = selectedCase.playerStats().values();
            for (CaseStat.PlayerStat playerStat : playerStats) {
                fetchProfile(playerStat.uuid());
            }
        }

        @Override
        protected void enableScissor(DrawContext context) {
            renderHeader(context, getRowLeft(), getY() - customHeaderHeight);

            super.enableScissor(context);
        }

        @Override
        protected void renderHeader(DrawContext context, int x, int y) {
            if (!this.client.mouse.wasLeftButtonClicked()) {
                this.selectedHeaderColumn = -1;
            }

            if (selectedCase == null) return;
            Text totalText = Text.literal("Gesamt");
            context.drawTextWithShadow(CaseStatsScreen.this.textRenderer, totalText, x + CaseStatsScreen.this.getColumnX(0) - CaseStatsScreen.this.textRenderer.getWidth(totalText), y + 5, 16777215);

            Text probText = Text.literal("%");
            context.drawTextWithShadow(CaseStatsScreen.this.textRenderer, probText, x + CaseStatsScreen.this.getColumnX(1) - CaseStatsScreen.this.textRenderer.getWidth(probText), y + 5, 16777215);

            int i = 2;
            for (CaseStat.PlayerStat playerStat : selectedCase.playerStats().values()) {
                Identifier identifier = this.selectedHeaderColumn == i ? CaseStatsScreen.SLOT_TEXTURE : CaseStatsScreen.HEADER_TEXTURE;
                CaseStatsScreen.this.renderIcon(context, x + CaseStatsScreen.this.getColumnX(i) - 18, y + 1, identifier);

                ItemStack itemStack = new ItemStack(Items.PLAYER_HEAD);
                fetchProfile(playerStat.uuid()).ifPresent(gameProfile -> {
                    NbtCompound nbt = itemStack.getOrCreateNbt();
                    nbt.put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), gameProfile));
                });

                context.drawItem(itemStack, x + CaseStatsScreen.this.getColumnX(i) - 17, y + 2);
                i++;
            }

//            if (this.selectedStatType != null) {
//                i = CaseStatsScreen.this.getColumnX(this.getHeaderIndex(this.selectedStatType)) - 36;
//                identifier = this.listOrder == 1 ? CaseStatsScreen.SORT_UP_TEXTURE : CaseStatsScreen.SORT_DOWN_TEXTURE;
//                CaseStatsScreen.this.renderIcon(context, x + i, y + 1, identifier);
//            }

        }

        protected void renderFooter(DrawContext context, int x, int y) {
            if (selectedCase == null) return;
            Text total = Text.literal("Gesamt:");
            context.drawTextWithShadow(CaseStatsScreen.this.textRenderer, total, x + CaseStatsScreen.this.textRenderer.getWidth(total), y + 5, 16777215);

            Text totalText = Text.literal(String.valueOf(selectedCase.total()));
            context.drawTextWithShadow(CaseStatsScreen.this.textRenderer, totalText, x + CaseStatsScreen.this.getColumnX(0) - CaseStatsScreen.this.textRenderer.getWidth(totalText), y + 5, 16777215);

            Text probText = Text.literal("100%");
            context.drawTextWithShadow(CaseStatsScreen.this.textRenderer, probText, x + CaseStatsScreen.this.getColumnX(1) - CaseStatsScreen.this.textRenderer.getWidth(probText), y + 5, 16777215);

            int i = 2;
            for (CaseStat.PlayerStat playerStat : selectedCase.playerStats().values()) {
                Text totalPlayerTxt = Text.literal(String.valueOf(playerStat.total()));
                context.drawTextWithShadow(CaseStatsScreen.this.textRenderer, totalPlayerTxt, x + CaseStatsScreen.this.getColumnX(i) - CaseStatsScreen.this.textRenderer.getWidth(totalPlayerTxt), y + 5, 16777215);
                i++;
            }
        }

        public int getRowWidth() {
            return 375;
        }

        protected int getScrollbarPositionX() {
            return this.width / 2 + 140;
        }

//        protected void clickedHeader(int x, int y) {
//            this.selectedHeaderColumn = -1;
//
//            for (int i = 0; i < this.headerIconTextures.length; ++i) {
//                int j = x - CaseStatsScreen.this.getColumnX(i);
//                if (j >= -36 && j <= 0) {
//                    this.selectedHeaderColumn = i;
//                    break;
//                }
//            }
//
//            if (this.selectedHeaderColumn >= 0) {
//                this.selectStatType(this.getStatType(this.selectedHeaderColumn));
//                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//            }
//
//        }

//        private StatType<?> getStatType(int headerColumn) {
//            return headerColumn < this.blockStatTypes.size() ? this.blockStatTypes.get(headerColumn) : this.itemStatTypes.get(headerColumn - this.blockStatTypes.size());
//        }

        protected void renderDecorations(DrawContext context, int mouseX, int mouseY) {
            renderFooter(context, getRowLeft(), getBottom() + 2);

            if (mouseY >= this.getY() - customHeaderHeight && mouseY <= this.getBottom()) {
                CaseStatsListWidget.Entry entry = this.getHoveredEntry();
                int i = (this.width - this.getRowWidth()) / 2;
                if (entry != null) {
                    if (mouseX < i + 40 || mouseX > i + 40 + 20) {
                        return;
                    }
                    context.drawItemTooltip(CaseStatsScreen.this.textRenderer, entry.getItem().stack(), mouseX, mouseY);
                } else {
                    int j = mouseX - i;

                    int k = 2;
                    for (CaseStat.PlayerStat playerStat : selectedCase.playerStats().values()) {
                        int l = CaseStatsScreen.this.getColumnX(k);
                        if (j >= l - 18 && j <= l) {
                            fetchProfile(playerStat.uuid()).ifPresent(gameProfile -> context.drawTooltip(CaseStatsScreen.this.textRenderer, Text.literal(gameProfile.getName()), mouseX, mouseY));
                            break;
                        }
                        k++;
                    }
                }
            }
        }

//        protected void selectStatType(StatType<?> statType) {
//            if (statType != this.selectedStatType) {
//                this.selectedStatType = statType;
//                this.listOrder = -1;
//            } else if (this.listOrder == -1) {
//                this.listOrder = 1;
//            } else {
//                this.selectedStatType = null;
//                this.listOrder = 0;
//            }
//
//            this.children().sort(this.comparator);
//        }

        @Environment(EnvType.CLIENT)
        private class ItemComparator implements Comparator<CaseStatsListWidget.Entry> {
            ItemComparator() {
            }

            public int compare(CaseStatsListWidget.Entry entry, CaseStatsListWidget.Entry entry2) {
//                CaseItem item = entry.getItem();
//                CaseItem item2 = entry2.getItem();
//                int itemStats;
//                int itemStats2;
//                if (selectedCase == null) {
//                    itemStats = 0;
//                    itemStats2 = 0;
//                } else {
//                    UUID selectedPlayer = UUID.randomUUID(); //TODO GETSELECTED
//                    itemStats = selectedCase.playerStats().get(selectedPlayer).getOccurrence(item);
//                    itemStats2 = selectedCase.playerStats().get(selectedPlayer).getOccurrence(item2);
//                }
//
//                return itemStats == itemStats2 ? CaseStatsScreen.CaseStatsListWidget.this.listOrder * Integer.compare(Item.getRawId(item.stack().getItem()), Item.getRawId(item2.stack().getItem())) : CaseStatsScreen.CaseStatsListWidget.this.listOrder * Integer.compare(itemStats, itemStats2);
                return 0;
            }
        }

        @Environment(EnvType.CLIENT)
        private class Entry extends AlwaysSelectedEntryListWidget.Entry<CaseStatsListWidget.Entry> {
            private final CaseItem item;

            Entry(CaseItem item) {
                this.item = item;
            }

            public CaseItem getItem() {
                return this.item;
            }

            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                CaseStatsScreen.this.renderStatItem(context, x + 40, y, this.item.stack());
                boolean evenRow = index % 2 == 0;
                int total = selectedCase.totals().getOrDefault(item, 0);

                int i = 2;
                for (CaseStat.PlayerStat playerStat : selectedCase.playerStats().values()) {
                    int count = playerStat.getOccurrence(item);
                    this.render(context, String.valueOf(count), x + CaseStatsScreen.this.getColumnX(i), y, evenRow);
                    i++;
                }

                render(context, String.valueOf(total), x + CaseStatsScreen.this.getColumnX(0), y, evenRow);

                int totalCount = selectedCase.total();
                double probability = (double) total / totalCount * 100;
                DecimalFormat formatter = new DecimalFormat("#,##0.00");

                render(context, formatter.format(probability) + "%", x + CaseStatsScreen.this.getColumnX(1), y, evenRow);
            }

            protected void render(DrawContext context, String count, int x, int y, boolean white) {
                Text text = count.equals("0") ? NONE_TEXT : Text.literal(count);
                context.drawTextWithShadow(CaseStatsScreen.this.textRenderer, text, x - CaseStatsScreen.this.textRenderer.getWidth(text), y + 5, white ? 16777215 : 9474192);
            }

            public Text getNarration() {
                return Text.translatable("narrator.select", this.item.stack());
            }
        }
    }

    public static class ItemStackButtonWidget extends ButtonWidget {

        private final ItemStack stack;

        ItemStackButtonWidget(int x, int y, int width, int height, ItemStack stack, PressAction onPress) {
            super(x, y, width, height, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
            this.stack = stack;
        }

        /*@Override
        protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderButton(context, mouseX, mouseY, delta);
            context.drawItem(stack, getX() + 2, getY() + 2);
        }*/

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderWidget(context, mouseX, mouseY, delta);

            boolean bl = this.hovered || this.isFocused() && MinecraftClient.getInstance().getNavigationType().isKeyboard();
            if (bl) {
                MatrixStack textMatrixStack = context.getMatrices();
                textMatrixStack.push();
                textMatrixStack.loadIdentity();
                textMatrixStack.translate(0, 0, 900);
                context.drawItemTooltip(MinecraftClient.getInstance().textRenderer, stack, mouseX, mouseY);
                textMatrixStack.pop();

            }
            context.drawItem(stack, getX() + 2, getY() + 2);
        }

        @Override
        public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        }
    }
}
