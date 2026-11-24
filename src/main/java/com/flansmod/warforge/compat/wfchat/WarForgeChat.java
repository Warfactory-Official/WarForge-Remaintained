package com.flansmod.warforge.compat.wfchat;

import com.wf.wfchat.api.ChannelSpec;
import com.wf.wfchat.api.ChatRegistry;
import com.wf.wfchat.api.Sender;
import com.wf.wfchat.api.WfChatApi;
import com.wf.wfchat.api.WfChatPlugin;
import com.wf.wfchat.api.WfChatServer;
import net.minecraft.network.chat.Component;

public final class WarForgeChat {
    public static final String FACTION = "warforge:faction";
    public static final String ALLY = "warforge:ally";

    private static boolean active;

    private WarForgeChat() {
    }

    public static void register() {
        active = true;
        WfChatApi.register(new WfChatPlugin() {
            @Override
            public String id() {
                return "warforge";
            }

            @Override
            public void register(ChatRegistry r) {
                r.channel(ChannelSpec.builder(FACTION).title("Faction").color(0x55FF55)
                        .order(40).priority(5).sender(Sender.command("f chat")).build());
                r.channel(ChannelSpec.builder(ALLY).title("Ally").color(0x55FFFF)
                        .order(50).priority(6).sender(Sender.command("f ally chat")).build());
                r.classifier((msg, sink) -> {
                    String text = msg.content().getString();
                    if (text.startsWith("[") && text.contains(" > Faction]")) {
                        sink.add(FACTION);
                        sink.claim();
                    } else if (text.startsWith("[") && text.contains(" > Ally]")) {
                        sink.add(ALLY);
                        sink.claim();
                    }
                });
            }
        });
    }

    public static Component faction(Component line) {
        return active ? WfChatServer.stamp(FACTION, line) : line;
    }

    public static Component ally(Component line) {
        return active ? WfChatServer.stamp(ALLY, line) : line;
    }
}
