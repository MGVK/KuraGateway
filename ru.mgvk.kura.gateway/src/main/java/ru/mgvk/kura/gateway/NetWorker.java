package ru.mgvk.kura.gateway;

import org.eclipse.kura.wire.WireRecord;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.regex.Pattern;

public class NetWorker {


    //    private static final Log                logger  = Log?
    private static final Pattern            PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private              Socket             client;
    private              OutputStream       os;
    private              ObjectOutputStream obos;
    private              ServerWorker       serverWorker;

    public NetWorker() {

    }

    public boolean initConnection(String ip, int port, int repeatTimes) {

        if (!(checkIP(ip) && checkPort(port))) {
            return false;
        }

        boolean result = true;

        try {
            client = new Socket(ip, port);
        } catch (Exception e) {
//            logger.info("Unable to connect to {}:{}", ip, port);
            if (repeatTimes > 0 && checkIP(ip) && checkPort(port)) {
                new Thread(() -> {
                    int count = repeatTimes;
                    while (!initConnection(ip, port, 0) && count-- > 0) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }).start();
            }
            result = false;
        }
        if (client != null && client.isConnected()) {
            try {
                os = client.getOutputStream();
                if (os != null) {
                    obos = new ObjectOutputStream(os);
                }
            } catch (Exception e) {
//                logger.info("Unable to open stream to {}:{}", ip, port);
                result = false;
            }
        } else {
            result = false;
        }
        if (result) {
//            logger.info("Connection to {}:{} established!", ip, port);
        }

        return result;
    }

    private boolean checkIP(String ip) {
        return PATTERN.matcher(ip).matches();
    }

    private boolean checkPort(int port) {
        return port > 1000 && port <= 65536;
    }

    public boolean sendData(int index, List<WireRecord> data) {
        boolean result = true;
//        logger.info("Sending data: {}", data.toString());
        if (os != null) {
            try {
//                obos.writeObject(new NetDataBundle().toSerializable(data));
                new NetDataBundle(index).toSerializable(data).writeObjectToStream(obos);
//                obos.flush();
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
            }
        }
        return result;
    }

    public boolean initServer(int port, OnDataReceiver onDataReceiver) {
        boolean result = true;
        if (onDataReceiver != null && checkPort(port)) {
            try {
                serverWorker = new ServerWorker(port, onDataReceiver);
            } catch (Exception e) {
                e.printStackTrace();
//                logger.info("Unable to start server! " + e.getMessage());
            }
        } else {
            result = false;
        }
        return result;
    }

    public void stopAll() {

//        logger.info("Stopping all connections...");

        if (obos != null) {
            try {
                obos.flush();
                obos.close();
            } catch (Exception e) {
            }
        }
        if (os != null) {
            try {
                os.flush();
                os.close();
            } catch (Exception e) {
            }
        }
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
            }
        }
        if (serverWorker != null) {
            try {
                serverWorker.stop();
            } catch (Exception e) {
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    public interface OnDataReceiver {

        void onRecieve(int wireIndex, List<WireRecord> wireRecords);

    }

    static class ServerWorker implements Runnable {

        private          Thread         thread;
        private          int            port = 0;
        private volatile ServerSocket   serverSocket;
        private          OnDataReceiver onDataReceiver;

        public ServerWorker(int port, OnDataReceiver onDataReceiver) {
            this.onDataReceiver = onDataReceiver;
            this.port = port;
            thread = new Thread(this);
            thread.start();
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
            } catch (Exception e) {
                e.printStackTrace();
//                logger.info("Server start failed: port={}; {}", port, e.getMessage());
            }

            while (!Thread.currentThread().isInterrupted()) {

                if (serverSocket == null) {
                    Thread.currentThread().interrupt();
                } else {
                    try (Socket client = serverSocket.accept()) {


                        InputStream is = null;
                        try {
                            is = client.getInputStream();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (is != null) {

                            ObjectInputStream obis = new ObjectInputStream(is);


                            while (client.isConnected()) {
                                try {
                                    NetDataBundle dataBundle = NetDataBundle.readFromStream(obis);
//                                    Object _data = obis.readObject();
                                    if (onDataReceiver != null) {
                                        onDataReceiver
                                                .onRecieve(dataBundle.getWireIndex(), dataBundle.fromSerializable());
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Thread.sleep(1000);
                                }
                            }

                        }


                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                        if (serverSocket != null || serverSocket.isClosed()) {
                            stop();
                        }
                    }
                }

            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        public void stop() {

            if (thread != null) {
                thread.interrupt();
                try {
                    serverSocket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}

