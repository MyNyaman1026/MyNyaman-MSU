package com.mynyaman.mynyamanapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThreatTextPipelineTest {

    @Test
    public void uiStatusAndProfileText_areFilteredBeforeGroq() {
        assertBlocked("Home", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Back", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Profile picture", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Active now", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_STATUS_TEXT);
        assertBlocked("Typing\u2026", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_STATUS_TEXT);
        assertBlocked("Loading\u2026", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_STATUS_TEXT);
        assertBlocked("Instagram Calling Dropdown", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Meta AI Side Chat", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Search or ask Meta AI", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Page 1 of 3 pages", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_PROFILE_METADATA);
        assertBlocked("Joined Apr 2026", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_PROFILE_METADATA);
        assertBlocked("1 follower \u00b7 0 posts", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_PROFILE_METADATA);
        assertBlocked("You're in sleep mode, so consider closing Instagram. We'll notify you later about any messages you get.",
                "UI_GATE_DROP",
                ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Enlarge photo", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Enlarge", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("View photo", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("View image", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Voice message, press and hold to record", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
    }

    @Test
    public void timestampMetadata_isFilteredBeforeGroq() {
        assertBlocked("Today", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Yesterday", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("YESTERDAY 23:05", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("TODAY 00:26", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Yesterday 11:30 PM", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Today 8:05 AM", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Monday", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("MONDAY 14:10", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("APR 24, 15:05", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("April 26", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("12:45", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("11:30 PM", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
    }

    @Test
    public void appNames_areFilteredBeforeGroq() {
        assertBlocked("Instagram", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("WhatsApp", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Messenger", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Telegram", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Gmail", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("TikTok", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("Facebook", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
    }

    @Test
    public void onlyActionableEnglishSafetyCues_areSentToGroq() {
        assertBlocked("What do you mean?", "MESSAGE_GATE_DROP_LOW_VALUE", ThreatTextGate.Decision.DROP_LOW_VALUE_NON_MESSAGE);
        assertBlocked("okay see you later", "MESSAGE_GATE_DROP_LOW_VALUE", ThreatTextGate.Decision.DROP_LOW_VALUE_NON_MESSAGE);
        assertBlocked("i want to kill you in Mobile Legends", "MESSAGE_GATE_DROP_LOW_VALUE", ThreatTextGate.Decision.DROP_LOW_VALUE_NON_MESSAGE);
        assertBlocked("come bro lets kill them in ML tonight", "MESSAGE_GATE_DROP_LOW_VALUE", ThreatTextGate.Decision.DROP_LOW_VALUE_NON_MESSAGE);
        assertAccepted("come bro lets bully john tomorrow", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("i want you to jump off and die you bitch", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("i will kill you", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("i will stab you", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("i will shoot you", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("stupid", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("you are stupid", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("send nude or I expose you", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("wait outside school tomorrow", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("lets rape a girl in Counter Strike", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
    }

    @Test
    public void bahasaMelayuAndRojakMessages_areSentToGroq() {
        assertAccepted("jangan kawan dia, dia bodoh", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("kau memang tak guna", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("kamu ni bodoh la", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("pergi mati lah", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("bunuh kau", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("aku tunggu kau lepas sekolah", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("nanti aku cari kau", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("jangan hidup lagi", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("hantar gambar atau aku sebarkan", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("kau ni loser gila", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertAccepted("jom buli dia esok", ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE);
        assertBlocked("aku bunuh kau dalam ML je", "MESSAGE_GATE_DROP_LOW_VALUE", ThreatTextGate.Decision.DROP_LOW_VALUE_NON_MESSAGE);
    }

    @Test
    public void harmlessShortTextAndHandles_areRejected() {
        assertBlocked("ok", "MESSAGE_GATE_DROP_LOW_VALUE", ThreatTextGate.Decision.DROP_SHORT_HARMLESS_FILLER);
        assertBlocked("hi", "MESSAGE_GATE_DROP_LOW_VALUE", ThreatTextGate.Decision.DROP_SHORT_HARMLESS_FILLER);
        assertBlocked("yes", "MESSAGE_GATE_DROP_LOW_VALUE", ThreatTextGate.Decision.DROP_SHORT_HARMLESS_FILLER);
        assertBlocked("@john_doe", "MESSAGE_GATE_DROP_LOW_VALUE", ThreatTextGate.Decision.DROP_USERNAME_ONLY);
        assertBlocked("john_123", "MESSAGE_GATE_DROP_LOW_VALUE", ThreatTextGate.Decision.DROP_USERNAME_ONLY);
        assertBlocked("message", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
        assertBlocked("photo", "UI_GATE_DROP", ThreatTextGate.Decision.DROP_UI_NOISE);
    }

    @Test
    public void duplicateSuppression_skipsRepeatedIgnoredAndAcceptedText() {
        ThreatTextPipeline pipeline = new ThreatTextPipeline();

        ThreatTextGate.GateResult firstIgnored = pipeline.preflight("Forward image", 1000L);
        ThreatTextGate.GateResult repeatedIgnored = pipeline.preflight("Forward image", 1500L);
        assertFalse(firstIgnored.accepted);
        assertEquals("UI_GATE_DROP", firstIgnored.logCode);
        assertFalse(repeatedIgnored.accepted);
        assertEquals("SKIPPED_DUPLICATE", repeatedIgnored.logCode);

        ThreatTextGate.GateResult message = pipeline.preflight("i will kill you", 3000L);
        assertTrue(message.accepted);
        ThreatTextPipeline.SubmissionResult firstSubmission = pipeline.beforeSubmit("i will kill you", 4000L);
        ThreatTextPipeline.SubmissionResult repeatedSubmission = pipeline.beforeSubmit("i will kill you", 4500L);
        assertTrue(firstSubmission.shouldSubmit);
        assertFalse(repeatedSubmission.shouldSubmit);
        assertEquals("SKIPPED_DUPLICATE", repeatedSubmission.logCode);
    }

    private void assertAccepted(String text, ThreatTextGate.Decision expectedDecision) {
        ThreatTextGate.GateResult result = ThreatTextGate.evaluate(text);
        assertTrue("Expected accepted: " + text + " but got " + result.logCode, result.accepted);
        assertEquals("MESSAGE_GATE_SEND_TO_GROQ", result.logCode);
        assertEquals(expectedDecision, result.decision);
    }

    private void assertBlocked(String text, String expectedLogCode, ThreatTextGate.Decision expectedDecision) {
        ThreatTextGate.GateResult result = ThreatTextGate.evaluate(text);
        assertFalse("Expected blocked: " + text, result.accepted);
        assertEquals(expectedLogCode, result.logCode);
        assertEquals(expectedDecision, result.decision);
    }
}
