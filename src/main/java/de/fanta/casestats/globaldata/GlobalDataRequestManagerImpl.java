package de.fanta.casestats.globaldata;

import de.cubeside.connection.GlobalServer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class GlobalDataRequestManagerImpl<T extends Enum<T>> implements GlobalDataRequestManager<T> {

    private static final long TIMEOUT_MS = 60L * 1000L; // 1 minute
    private static final Timer timeoutTimer = new Timer();

    private class Request<V> implements Future<V> {

        private final Object lock;
        private boolean cancelled;
        private boolean responseReceived;
        private boolean done;
        private Throwable thrown;
        private V result;

        private TimerTask timeoutTask;
        private UUID requestId;

        private Request(UUID requestId) {
            this.lock = new Object();
            this.requestId = requestId;
            this.timeoutTask = new TimerTask() {

                @Override
                public void run() {
                    synchronized (Request.this.lock) {
                        if (Request.this.responseReceived || Request.this.cancelled) {
                            return;
                        }

                        GlobalDataRequestManagerImpl.this.activeRequests.remove(Request.this.requestId, Request.this);
                        Request.this.responseReceived = true;
                        Request.this.done = true;
                        Request.this.thrown = new TimeoutException();
                        Request.this.lock.notifyAll();
                    }
                }
            };

            timeoutTimer.schedule(timeoutTask, TIMEOUT_MS);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (!mayInterruptIfRunning) {
                return false;
            }
            synchronized (this.lock) {
                if (this.responseReceived) {
                    return false;
                }
                this.cancelled = true;
                this.done = true;
                this.timeoutTask.cancel();
                this.lock.notifyAll();
                return true;
            }
        }

        @Override
        public boolean isCancelled() {
            synchronized (this.lock) {
                return this.cancelled;
            }
        }

        @Override
        public boolean isDone() {
            synchronized (this.lock) {
                return this.done;
            }
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            synchronized (this.lock) {
                while (!this.done) {
                    this.lock.wait();
                }

                if (this.cancelled) {
                    throw new CancellationException();
                }

                return getResult();
            }
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            long start = System.currentTimeMillis();
            synchronized (this.lock) {
                while (timeout > 0 && !this.done) {
                    this.lock.wait(unit.toMillis(timeout));
                    long now = System.currentTimeMillis();
                    timeout = (now - start);
                    start = now;
                }

                if (this.cancelled) {
                    throw new CancellationException();
                }
                if (!this.done) {
                    throw new TimeoutException();
                }

                return getResult();
            }
        }

        private V getResult() throws ExecutionException {
            if (this.thrown != null) {
                throw new ExecutionException(thrown);
            }

            return this.result;
        }

        public void set(V result) {
            synchronized (this.lock) {
                if (this.done || !this.responseReceived) {
                    throw new IllegalStateException();
                }
                this.result = result;
                this.done = true;
                this.timeoutTask.cancel();
                this.lock.notifyAll();
            }
        }

        public void error(Throwable thrown) {
            synchronized (this.lock) {
                if (this.done || !this.responseReceived) {
                    throw new IllegalStateException();
                }
                this.thrown = thrown;
                this.done = true;
                this.timeoutTask.cancel();
                this.lock.notifyAll();
            }
        }

        public boolean setResponseReceived() {
            synchronized (this.lock) {
                if (this.cancelled) {
                    return false;
                }
                this.responseReceived = true;
                return true;
            }
        }
    }

    protected static class Delegator<T extends Enum<T>> {
        private GlobalDataRequestManagerImpl<T> requestManager;

        public Delegator() {

        }

        public void handleMessage(T messageType, GlobalServer source, DataInputStream data) throws IOException {
            if (this.requestManager == null) {
                throw new IllegalStateException();
            }
            this.requestManager.handleMessage(messageType, source, data);
        }

        private void setRequestManager(GlobalDataRequestManagerImpl<T> requestManager) {
            if (this.requestManager != null) {
                throw new IllegalStateException();
            }
            this.requestManager = Objects.requireNonNull(requestManager);
        }
    }

    private GlobalDataHelperImpl<T> helper;
    private Map<UUID, Request<?>> activeRequests;

    public GlobalDataRequestManagerImpl(Pair<GlobalDataHelperImpl<T>, Delegator<T>> helperAndDelegator) {
        this.helper = helperAndDelegator.first;
        this.activeRequests = Collections.synchronizedMap(new HashMap<>());
        helperAndDelegator.second.setRequestManager(this);
    }

    protected GlobalDataHelperImpl<T> getHelper() {
        return this.helper;
    }

    @Override
    public <V> Future<V> makeRequest(T requestType, GlobalServer server, Object... data) {
        UUID requestId = UUID.randomUUID();
        Request<V> request = new Request<>(requestId);
        this.activeRequests.put(requestId, request);

        Object[] requestData = new Object[data.length + 2];
        requestData[0] = false;
        requestData[1] = requestId;
        System.arraycopy(data, 0, requestData, 2, data.length);

        getHelper().sendData(server, requestType, requestData);
        return request;
    }

    @SuppressWarnings("unchecked")
    protected void handleMessage(T messageType, GlobalServer source, DataInputStream data) throws IOException {
        ByteArrayOutputStream responseBytes = new ByteArrayOutputStream();
        DataOutputStream responseOut = new DataOutputStream(responseBytes);
        boolean isResponse = data.readBoolean();
        UUID requestId = readUUID(data);

        if (isResponse) {
            Request<Object> request = (Request<Object>) this.activeRequests.remove(requestId);
            if (request == null) {
                throw new NoSuchElementException("unknown request id");
            }
            if (!request.setResponseReceived()) {
                return;
            }

            try {
                request.set(handleResponse(messageType, source, data));
            } catch (Throwable t) {
                request.error(t);
            }
        } else {
            responseOut.writeInt(messageType.ordinal());
            responseOut.writeBoolean(true);
            sendMsgPart(responseOut, requestId);

            respondToRequest(messageType, source, data, responseOut);

            byte[] msgarry = responseBytes.toByteArray();
            source.sendData(getHelper().getChannel(), msgarry);
        }

    }

    protected void sendMsgPart(DataOutputStream msgout, Object msg) throws IOException {
        getHelper().sendMsgPart(msgout, msg);
    }

    protected void sendMsgParts(DataOutputStream msgout, Object... msgs) throws IOException {
        for (Object msg : msgs) {
            sendMsgPart(msgout, msg);
        }
    }

    protected UUID readUUID(DataInputStream msgin) throws IOException {
        return getHelper().readUUID(msgin);
    }

    protected <S extends StringSerializable> S readStringSerializable(DataInputStream msgin) throws IOException {
        return getHelper().readStringSerializable(msgin);
    }

    protected abstract void respondToRequest(T requestType, GlobalServer source, DataInputStream requestData, DataOutputStream responseData) throws IOException;

    protected abstract Object handleResponse(T requestType, GlobalServer source, DataInputStream responseData) throws IOException;

}
