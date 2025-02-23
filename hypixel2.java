int blockTicks, cooldown, overallTicks, change, blocksTicksInteract;
int swapSlot, swordSlot, unblockSlot;
boolean blocked, start, swapped, disable, dispatch, setSwordSlot;
List<CPacket> blinkPackets = Collections.synchronizedList(new ArrayList<>());
List<CPacket> regularPackets = Collections.synchronizedList(new ArrayList<>());
final String[] elevatedUsers = new String[] {"vivivox"};
boolean shouldRun = false;
void onLoad() {
    String currentUser = client.getUser();
    for (String user : elevatedUsers) {
        if (user.equals(currentUser)) {
            shouldRun = true;
            break;
        }
    }
    if (shouldRun) {
        modules.registerSlider("Mode", "",  0, new String[]{"Normal", "Blink", "Hypixel", "Extra tick", "Interact"});
        modules.registerSlider("Attack frequency", " ticks", 4, 2, 10, 1);
        modules.registerSlider("Unblock delay", " ticks", 1, 1, 5, 1);
        modules.registerButton("force block animation", false);
        modules.registerButton("only on rmb", false);
    } else {
        modules.registerButton("UID not whitelisted", false);
        client.print("&cUID not whitelisted for hypixel2 script");
        modules.disable(scriptName);
    }
}
boolean onMouse(int button, boolean state) {
    if (!shouldRun) return true;
    if (button >= 0 && state && function()) {
        return false;
    }
    return true;
}
boolean onPacketSent(CPacket packet) {
    if (!shouldRun) return true;
    Entity player = client.getPlayer();
    if ((function() || start) && (packet instanceof C02 || packet instanceof C0A || packet instanceof C08)) {
        return false;
    }
    if ((function() || start) && packet instanceof C07) {
        C07 c07 = (C07) packet;
        if (c07.status.equals("RELEASE_USE_ITEM")) return false;
    }
    if (!start) {
        if (packet instanceof C09) {
            C09 c09 = (C09) packet;
            if (c09.slot != swordSlot && swapped && inventory.getSlot() == swapSlot) {
                swapped = false;
                //client.print("dupe?");
                //client.print("cancelled dupe slot");
                return false;
            } else if (swapped) {
                swapped = false;
               //client.print("c09 switch");
            }
        }
    }
    if (function() && start && !disable) {
        if (packet instanceof C09) {
            C09 c09 = (C09) packet;
            if (c09.slot != swordSlot) {
                disable = true;
                blocked = false;
                cooldown = 0;
               //client.print("c09 dif");
                //client.print("disabled (c09)");
            }
        }/* else if (packet instanceof C07) {
            disable = true;
            blocked = false;
            cooldown = 0;
           //client.print("c07");
        }*/
    }
    if (function()) {
        if (!disable) {
            if (modules.getButton(scriptName, "force block animation") && blocked) keybinds.rightClick();
            if (packet.name.contains("Keep Alive") || packet.name.contains("Login") || packet.name.contains("Handshake")) return true;
            blinkPackets.add(packet);
            regularPackets.add(packet);
            return false;
        }
    }
    return true;
}
void onPreUpdate() {
    if (!shouldRun) return;
    Entity player = client.getPlayer();
    randomizeSlots();
    if (disable && ++cooldown >= 2) {
        blocked = dispatch = swapped = false;
        disable = false;
        cooldown = blockTicks = change = blockTicks = 0;
    }
    if (function()) {
        if (!setSwordSlot) {
            if (sword()) swordSlot = inventory.getSlot();
            setSwordSlot = true;
        }
        if (!disable) blockTicks++;
        overallTicks++;
        start = true;
        if (!disable) {
            switch((int) modules.getSlider(scriptName, "Mode")) {
            case 0:
                if (canUnblock()) {
                    if (blocked) {
                        swapSlot = unblockSlot;
                        client.sendPacketNoEvent(new C09(swapSlot));
                        swapped = true;
                        if (!modules.getButton(scriptName, "force block animation")) keybinds.setPressed("use", false);
                    }
                } else if (canBlock()) {
                    swapSlot = swordSlot;
                    if (blocked) client.sendPacketNoEvent(new C09(swapSlot));
                    swapped = false;
                    if (inAttackRange()) {
                        client.swing(false);
                        client.sendPacketNoEvent(new C0A());
                        client.sendPacketNoEvent(new C02(modules.getKillAuraTarget(), "ATTACK", null));
                        client.sendPacketNoEvent(new C02(modules.getKillAuraTarget(), "INTERACT", null));
                    }
                    client.sendPacketNoEvent(new C08(player.getHeldItem(), new Vec3(-1, -1, -1), 255, new Vec3(0, 0, 0)));
                    if (!modules.getButton(scriptName, "force block animation")) keybinds.setPressed("use", true);
                    blocked = true;
                    blockTicks = 0;
                }
                break;
            case 1:
                if (canUnblock()) {
                    if (blocked && !swapped) {
                        //swapSlot = unblockSlot;
                        //blinkPackets.add(new C09(swapSlot));
                        //swapped = true;
                        if (!modules.getButton(scriptName, "force block animation")) keybinds.setPressed("use", false);
                    }
                } else if (canBlock()) {
                    if (blocked && swapped) {
                        //swapSlot = swordSlot;
                        //blinkPackets.add(new C09(swapSlot));
                        //swapped = false;
                    }
                    if (inAttackRange()) {
                        client.swing(false);
                        blinkPackets.add(new C0A());
                        blinkPackets.add(new C02(modules.getKillAuraTarget(), "ATTACK", null));
                        blinkPackets.add(new C02(modules.getKillAuraTarget(), "INTERACT", null));
                    }
                    blinkPackets.add(new C08(player.getHeldItem(), new Vec3(-1, -1, -1), 255, new Vec3(0, 0, 0)));
                    if (!modules.getButton(scriptName, "force block animation")) keybinds.setPressed("use", true);
                    blocked = dispatch = true;
                    blockTicks = 0;
                }
                break;
            case 2: // Hypixel mode
                if (blockTicks >= 2) {
                    //swapSlot = unblockSlot;
                    //blinkPackets.add(new C09(swapSlot));
                    //swapSlot = swordSlot;
                    //blinkPackets.add(new C09(swapSlot));
                    if (inAttackRange()) {
                        client.swing(false);
                        blinkPackets.add(new C0A());
                        blinkPackets.add(new C02(modules.getKillAuraTarget(), "ATTACK", null));
                        blinkPackets.add(new C02(modules.getKillAuraTarget(), "INTERACT", null));
                    }
                    blinkPackets.add(new C08(player.getHeldItem(), new Vec3(-1, -1, -1), 255, new Vec3(0, 0, 0)));
                    blocked = dispatch = true;
                    blockTicks = 0;
                }
                break;
            case 3:
                if (blockTicks == 2) {
                    if (blocked && !swapped) {
                        //swapSlot = unblockSlot;
                        //blinkPackets.add(new C09(swapSlot));
                        //swapped = true;
                        if (!modules.getButton(scriptName, "force block animation")) keybinds.setPressed("use", false);
                    }
                } else if (blockTicks == 3 || blockTicks <= 1 && !blocked) {
                    if (blocked && swapped) {
                        //swapSlot = swordSlot;
                        //blinkPackets.add(new C09(swapSlot));
                        //swapped = false;
                    }
                    if (inAttackRange()) {
                        client.swing(false);
                        blinkPackets.add(new C0A());
                        blinkPackets.add(new C02(modules.getKillAuraTarget(), "ATTACK", null));
                        blinkPackets.add(new C02(modules.getKillAuraTarget(), "INTERACT", null));
                    }
                    blinkPackets.add(new C08(player.getHeldItem(), new Vec3(-1, -1, -1), 255, new Vec3(0, 0, 0)));
                    if (!modules.getButton(scriptName, "force block animation")) keybinds.setPressed("use", true);
                    blocked = dispatch = true;
                    blockTicks = 0;
                    change = 0;
                }
                break;
            case 4:
                if (blockTicks == 2) {
                    if (blocked && !swapped) {
                        swapSlot = unblockSlot;
                        blinkPackets.add(new C09(swapSlot));
                        swapped = true;
                        if (!modules.getButton(scriptName, "force block animation")) keybinds.setPressed("use", false);
                    }
                } else if (blockTicks == 3 || blockTicks <= 1 && !blocked) {
                    if (blocked && swapped) {
                        swapSlot = swordSlot;
                        blinkPackets.add(new C09(swapSlot));
                        swapped = false;
                    }
                    if (inAttackRange()) {
                        client.swing(false);
                        blinkPackets.add(new C0A());
                        blinkPackets.add(new C02(modules.getKillAuraTarget(), "ATTACK", null));
                        blinkPackets.add(new C02(modules.getKillAuraTarget(), "INTERACT", null));
                    }
                    blinkPackets.add(new C08(player.getHeldItem(), new Vec3(-1, -1, -1), 255, new Vec3(0, 0, 0)));
                    if (!modules.getButton(scriptName, "force block animation")) keybinds.setPressed("use", true);
                    blocked = dispatch = true;
                    blockTicks = 0;
                    change = 0;
                }
            }
        }
        if (modules.isEnabled("Sprint") && client.isMoving()) {
            client.setSprinting(true);
            keybinds.setPressed("sprint", true);
        }
    } else if (start) {
        blockTicks = overallTicks = 0;
        if (!regularPackets.isEmpty()) {
            synchronized (regularPackets) {
                for (CPacket packet : regularPackets) {
                    client.sendPacketNoEvent(packet);
                    //client.print("dispatched regular packets");
                }
            }
        }
        regularPackets.clear();
        blinkPackets.clear();
        if (blocked && sword() && !disable) {
            client.sendPacketNoEvent(new C07(new Vec3(0, 0, 0), "RELEASE_USE_ITEM", "DOWN"));
        }
        blocked = start = dispatch = disable = setSwordSlot = false;
        keybinds.setPressed("use", false);
       //client.print("unblock");
    }
    if (disable) {
        if (!regularPackets.isEmpty()) {
            synchronized (regularPackets) {
                for (CPacket packet : regularPackets) {
                    client.sendPacketNoEvent(packet);
                   //client.print("dispatched regular packets");
                }
            }
        }
        regularPackets.clear();
        blinkPackets.clear();
        dispatch = false;
    } else if (dispatch) {
        if (!blinkPackets.isEmpty()) {
            synchronized (blinkPackets) {
                for (CPacket packet : blinkPackets) {
                    client.sendPacketNoEvent(packet);
                   //client.print("dispatched blink packets");
                }
            }
        }
        blinkPackets.clear();
        regularPackets.clear();
        dispatch = false;
    }
}
boolean sword() {
    Entity player = client.getPlayer();
    return player.getHeldItem() != null && player.getHeldItem().type.contains("Sword");
}
boolean function() {
    return (!modules.getButton(scriptName, "only on rmb") || modules.getButton(scriptName, "only on rmb") && keybinds.isMouseDown(1)) && modules.isEnabled("KillAura") && modules.getKillAuraTarget() != null && sword() && !modules.isEnabled("Blink");
}
boolean inAttackRange() {
    if (modules.getKillAuraTarget() == null) return false;
    return client.getPlayer().getPosition().distanceTo(modules.getKillAuraTarget().getPosition()) <= 3.19;//3.39/3.11
}
boolean canUnblock() {
    return blockTicks == modules.getSlider(scriptName, "Attack frequency") - 1 && overallTicks > 2;
}
boolean canBlock() {
    return blockTicks >= modules.getSlider(scriptName, "Attack frequency") + modules.getSlider(scriptName, "Unblock delay") - 1 && overallTicks > 2;
}
void onDisable() {
    if (blocked && sword()) {
        client.sendPacketNoEvent(new C07(new Vec3(0, 0, 0), "RELEASE_USE_ITEM", "DOWN"));
    }
    blocked = start = dispatch = disable = setSwordSlot = false;
    blockTicks = overallTicks = change = 0;
}
int randomize = 0;
void randomizeSlots() {
    randomize++;
    if (randomize == 1) {
        unblockSlot = inventory.getSlot() % 8 + 1;
    } else if (randomize == 2) {
        unblockSlot = inventory.getSlot() % 7 + 2;
    } else if (randomize == 3) {
        unblockSlot = inventory.getSlot() % 6 + 3;
    } else if (randomize == 4) {
        unblockSlot = inventory.getSlot() % 5 + 4;
    } else if (randomize == 5) {
        unblockSlot = inventory.getSlot() % 4 + 5;
    } else if (randomize == 6) {
        unblockSlot = inventory.getSlot() % 3 + 6;
    } else if (randomize == 7) {
        unblockSlot = inventory.getSlot() % 2 + 7;
    } else if (randomize >= 8) {
        unblockSlot = inventory.getSlot() % 1 + 8;
        randomize = 0;
    }
}
