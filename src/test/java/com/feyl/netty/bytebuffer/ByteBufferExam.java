package com.feyl.netty.bytebuffer;

import java.nio.ByteBuffer;

import static com.feyl.netty.util.ByteBufferUtil.debugAll;

/**
 *  黏包/半包 现象：
 *  网络上有多条数据发送给服务端，数据之间使用 \n 进行分隔
 *             但由于某种原因这些数据在接收时，被进行了重新组合，例如原始数据有3条为
 *                 Hello,world\n
 *                 I'm zhangsan\n
 *                 How are you?\n
 *             变成了下面的两个 byteBuffer (黏包，半包)
 *                 Hello,world\nI'm zhangsan\nHo
 *                 w are you?\n
 *             现在要求你编写程序，将错乱的数据恢复成原始的按 \n 分隔的数据
 *
 *  黏包/半包 分析 解析
 *
 * @author Feyl
 * @date 2022/5/26 16:52
 */
public class ByteBufferExam {
    public static void main(String[] args) {
        ByteBuffer source = ByteBuffer.allocate(32);
        source.put("Hello,world\nI'm zhangsan\nHo".getBytes());
        split(source);
        source.put("w are you?\n".getBytes());
        split(source);
    }

    private static void split(ByteBuffer source) {
        source.flip(); //读模式
        for (int i = 0; i < source.limit(); i++) {
            // 找到一条完整消息
            if (source.get(i) == '\n') {
                int length = i - source.position();
                // 把这条完整消息存入新的 ByteBuffer
                ByteBuffer target = ByteBuffer.allocate(length);
                // 从 source 读，向 target 写
                for (int j = 0; j < length; j++) {
                    target.put(source.get());
                }
                debugAll(target);
            }
        }
        source.compact(); //写模式
    }
}
