package com.flamexander.netty.example.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.concurrent.Semaphore;


public class Handler1 extends ChannelInboundHandlerAdapter {
    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }
    private static final byte SIGNAL_BYTE_MESSAGE =20;
    private static final byte SIGNAL_BYTE_FILE=25;

    private State currentState = State.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;
    private String name;
                                // контекст  - вся информация о соединении с клиентом
    @Override                   // ссылка на контекст  +  посылка
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        byte readed=0;
        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                 readed = buf.readByte();
                if (readed == SIGNAL_BYTE_FILE || readed== SIGNAL_BYTE_MESSAGE) {
                    currentState = State.NAME_LENGTH;
                    receivedFileLength = 0L;
                    System.out.println("STATE: Start file receiving");
                } else {
                    System.out.println("ERROR: Invalid first byte - " + readed);
                }
            }
            if (currentState == State.NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get filename length");
                    nextLength = buf.readInt();
                    currentState = State.NAME;
                }
            }
            if (currentState == State.NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    name=new String(fileName);
                    System.out.println("SERVER STATE: Filename received in server: " + name);

//                    System.out.println("SERVER STATE: Filename received in server: " + new String(fileName, "UTF-8"));
//                    out = new BufferedOutputStream(new FileOutputStream("server_storage/" + new String(fileName)));
                        if (readed == SIGNAL_BYTE_FILE) {
                            out = new BufferedOutputStream(new FileOutputStream("server_storage/" + new String(fileName)));
                            currentState = State.FILE_LENGTH;
                        }
                        if(readed== SIGNAL_BYTE_MESSAGE) {
                            ctx.fireChannelRead(fileName);  // толкаем дальше
                            currentState = State.IDLE;
                        }
                }
            }
            if (currentState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    System.out.println("Server STATE: File length received - " + fileLength);
                    currentState = State.FILE;
                }
            }
            if (currentState == State.FILE) {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    System.out.println("Out сработал");
                    receivedFileLength++;
                    if (fileLength == receivedFileLength) {
                        currentState = State.IDLE;
                        System.out.println("File received");
                        out.close();
                        break;
                    }
                }
            }
        }
        if (buf.readableBytes() == 0) {
           buf.release();

        }
   //
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace(); // обязательно делать токое переопределение чтобы знать
        ctx.close();             // что произошло
        System.out.println(" ошибки при передаче в 1 хендлере");
    }


}
