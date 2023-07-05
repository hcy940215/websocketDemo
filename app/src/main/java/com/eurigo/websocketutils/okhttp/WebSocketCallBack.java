package com.eurigo.websocketutils.okhttp;

import okio.ByteString;

public interface WebSocketCallBack {

        void onOpen();

        void onMessage(String text);

        void onClose();

        void onConnectError(Throwable t);
}
