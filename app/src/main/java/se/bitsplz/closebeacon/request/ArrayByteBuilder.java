package se.bitsplz.closebeacon.request;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Formatter;
import java.util.Random;
import java.util.UUID;

import se.bitsplz.closebeacon.service.Storage;
import se.bitsplz.closebeacon.activity.DeviceScanActivity;

/**
 * @author jonnakollin
 * @author j0na5L
 */
public class ArrayByteBuilder {

    private byte[] authReq;
    private byte[] activationReq;
    private byte[] activateBeacon;
    private String okActivateResponse;
    private String okAuthResponse;
    private byte[] authKey;
    private byte[] majorNumber;
    private byte[] minorNumber;
    private byte[] uuidBytes;

    private byte[] adminKey = "abcdefghij0123456789".getBytes();
    private byte[] mobileKey = "0123456789abcdefghij".getBytes();


    public byte[] authBuilder(String authKeyString) {

        this.authKey = authKeyString.getBytes();
        byte[] version = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(1).array();
        byte[] randomByte = new byte[4];
        final Random random = new SecureRandom();
        random.nextBytes(randomByte);

        authReq = ByteBuffer.allocate(20).put(version).put(authKey).put(randomByte).array();
        okAuthResponse = responseBuilder(ByteBuffer.allocate(22).put(authReq).put("OK".getBytes()).array());

        return authReq;
    }

    private String responseBuilder(byte[] bytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            return byteToHex(messageDigest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            e.getMessage();
        }
        return "";
    }


    public byte[] activationBuilder(String deviceAddress, String major, String minor, String beaconUUID) {
        this.authKey = Storage.readString(DeviceScanActivity.getAppContext(), "LoginKey", "").getBytes();

        uuidBytes = uuidToBytes(beaconUUID);
        byte[] version = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(1).array();
        byte[] macAddress = macToBytes(deviceAddress);
        byte beaconType = 100;
        majorNumber = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(Short.parseShort(major)).array();
        minorNumber = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(Short.parseShort(minor)).array();

        activationReq = ByteBuffer.allocate(67).put(version).put(authKey).put(macAddress).put(adminKey).put(mobileKey).put(beaconType).put(majorNumber).put(minorNumber).array();
        okActivateResponse = responseBuilder(ByteBuffer.allocate(69).put(activationReq).put("OK".getBytes()).array());

        return activationReq;
    }

    public byte[] beaconActivationBuilder(String sha1) {

        byte commandCode = 80;
        byte beaconType = 100;
        byte[] appleId = ByteBuffer.allocate(2).put((byte) 0x004C).array();
        byte id1 = 2;
        byte id2 = 21;
        byte power = (byte) 0xC5;
        byte[] sha1Hash = hexStringToByteArray(sha1);
        activateBeacon = ByteBuffer.allocate(91).put(commandCode)
                .put(adminKey)
                .put(mobileKey)
                .put(beaconType)
                .put(majorNumber)
                .put(minorNumber)
                .put(sha1Hash)
                .put(appleId)
                .put(id1)
                .put(id2)
                .put(uuidBytes)
                .put(majorNumber)
                .put(minorNumber)
                .put(power).array();

        return activateBeacon;
    }

    public String getOkAuthResponse() {
        return okAuthResponse;
    }

    public String getOkActivateResponse() {
        return okActivateResponse;
    }

    public static String byteToHex(final byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public static byte[] macToBytes(String macAddress) {
        String[] macAddressArray = macAddress.split(":");
        byte[] macAddressBytes = new byte[macAddressArray.length];
        for (int i = 0; i < macAddressArray.length; i++) {
            macAddressBytes[i] = Integer.decode("0x" + macAddressArray[i]).byteValue();
        }
        return macAddressBytes;
    }

    public byte[] uuidToBytes(String uuid) {
        UUID beaconUUID = UUID.fromString(uuid);
        return ByteBuffer.allocate(16).putLong(beaconUUID.getMostSignificantBits()).putLong(beaconUUID.getLeastSignificantBits()).array();
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public String createSerialNumber(String macAddress) {

        String[] macAddressArray = macAddress.split(":");
        byte[] macAddressAsByte = new byte[6];
        int[] serialNumberAsInt = new int[6];
        StringBuilder createdSerialNumber = new StringBuilder();

        for (int i = 0; i < macAddressArray.length; i++) {
            macAddressAsByte[i] = Integer.decode("0x" + macAddressArray[i]).byteValue();
            serialNumberAsInt[i] = macAddressAsByte[i] & 0xff;
            String stringToAppend = String.format("%03d", serialNumberAsInt[i]);

            createdSerialNumber.append(stringToAppend);
            if (i < macAddressArray.length - 1) {
                createdSerialNumber.append("-");
            }
        }

        return createdSerialNumber.toString();
    }
}