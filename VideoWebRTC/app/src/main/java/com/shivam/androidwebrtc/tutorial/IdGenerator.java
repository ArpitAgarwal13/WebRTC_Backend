package com.shivam.androidwebrtc.tutorial;

import java.util.Random;

public class IdGenerator {

        private static final String CHARACTERS = "0123456789";
        private static final int LENGTH = 3;
        private static final Random random = new Random();

        public static String generatePeerId() {
            StringBuilder peerId = new StringBuilder();
            peerId.append("peer1");
            return peerId.toString();
//            for (int i = 0; i < LENGTH; i++) {
//                peerId.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
//            }
//            return peerId.toString();
        }

        public static String generateSessionId() {
            StringBuilder peerId = new StringBuilder();
            peerId.append("session1");
            return peerId.toString();
//            for (int i = 0; i < LENGTH; i++) {
//                peerId.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
//            }
//            return peerId.toString();
        }
}
