package com.tooploox.ussd.ui;

import android.util.Base64;

import com.tooploox.ussd.domain.Ussd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 02/03/2017 19:00
 */
public class Serializer {

    public static <T> String serialize(T object) {
        String base64 = "";
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.close();
            byte[] encodedBytes = Base64.encode(baos.toByteArray(), Base64.DEFAULT);
            base64 = new String(encodedBytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(oos);
            close(baos);
        }
        return base64;
    }

    public static <T> T deserialize(String base64) {
        T result = null;
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            result = (T) ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            close(ois);
        }
        return result;
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {}
        }
    }
}
