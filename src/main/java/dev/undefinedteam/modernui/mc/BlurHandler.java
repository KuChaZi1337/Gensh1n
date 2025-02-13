/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.undefinedteam.modernui.mc;

import dev.undefinedteam.gensh1n.mixins.modernui.ui.AccessPostEffectProcessor;
import icyllis.modernui.ModernUI;
import icyllis.modernui.animation.ColorEvaluator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Handling the blur effect of screen background. Client only.
 */
public enum BlurHandler {
    INSTANCE;

    // minecraft namespace
    private static final Identifier BLUR_POST_EFFECT = new Identifier("shaders/post/blur_fast.json");

    /**
     * Config values
     */
    public static volatile boolean sBlurEffect;
    public static volatile boolean sBlurWithBackground;
    public static volatile int sBlurRadius;
    public static volatile int sBackgroundDuration; // milliseconds
    public static volatile int[] sBackgroundColor = new int[4];

    public static volatile int sFramerateInactive;
    public static volatile int sFramerateMinimized;
    public static volatile float sMasterVolumeInactive = 1;
    public static volatile float sMasterVolumeMinimized = 1;

    private final MinecraftClient minecraft = MinecraftClient.getInstance();

    private volatile ArrayList<Class<? extends Screen>> mBlacklist = new ArrayList<>();

    private final int[] mBackgroundColor = new int[4];

    /**
     * If it is playing animation
     */
    private boolean mFadingIn;

    /**
     * If blur is running
     */
    private boolean mBlurring;

    /**
     * If blur post-processing shader is activated
     */
    private boolean mBlurLoaded;

    private float mBlurRadius;

    /**
     * If a screen excluded, the other screens that opened after this screen won't be blurred, unless current screen
     * closed
     */
    private boolean mHasScreen;

    private float mVolumeMultiplier = 1;

    /**
     * Use blur shader in game renderer post-processing.
     */
    public void blur(@Nullable Screen nextScreen) {
        if (minecraft.world == null) {
            return;
        }
        boolean hasScreen = nextScreen != null;

        boolean blocked = false;
        if (hasScreen && sBlurEffect) {
            if (nextScreen instanceof MuiScreen screen) {
                ScreenCallback callback = screen.getCallback();
                if (callback != null) {
                    blocked = !callback.shouldBlurBackground();
                }
            } else {
                final Class<?> t = nextScreen.getClass();
                for (Class<?> c : mBlacklist) {
                    if (c.isAssignableFrom(t)) {
                        blocked = true;
                        break;
                    }
                }
            }
        }

        if (blocked && mBlurring) {
            if (mBlurLoaded) {
                minecraft.gameRenderer.disablePostProcessor();
            }
            mFadingIn = false;
            mBlurring = false;
            mBlurLoaded = false;
        }

        GameRenderer gr = minecraft.gameRenderer;
        if (hasScreen && !mHasScreen) {
            if (!blocked && sBlurEffect && !mBlurring && gr.getPostProcessor() == null && sBlurRadius >= 1) {
                mBlurring = true;
                if (sBackgroundDuration > 0 && sBlurWithBackground) {
                    updateRadius(1);
                } else {
                    MuiModApi.get().loadEffect(minecraft.gameRenderer, BLUR_POST_EFFECT);
                    updateRadius(sBlurRadius);
                    mBlurLoaded = true;
                }
            }
            if (sBackgroundDuration > 0) {
                mFadingIn = true;
                Arrays.fill(mBackgroundColor, 0);
            } else {
                mFadingIn = false;
                System.arraycopy(sBackgroundColor, 0, mBackgroundColor, 0, 4);
            }
        } else if (!hasScreen) {
            if (mBlurring) {
                if (mBlurLoaded) {
                    gr.disablePostProcessor();
                }
                mBlurLoaded = false;
                mBlurring = false;
            }
            mFadingIn = false;
        }
        mHasScreen = hasScreen;
    }

    /**
     * Internal method, to re-blur after resources (including shaders) reloaded in the pause menu.
     */
    public void forceBlur() {
        // no need to check if is excluded, this method is only called by opened ModernUI Screen
        if (!sBlurEffect) {
            return;
        }
        if (minecraft.world != null && mBlurring) {
            GameRenderer gr = minecraft.gameRenderer;
            if (gr.getPostProcessor() == null) {
                MuiModApi.get().loadEffect(gr, BLUR_POST_EFFECT);
                mFadingIn = true;
                mBlurring = true;
                mBlurLoaded = true;
            } else {
                mBlurLoaded = false;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void loadBlacklist(@Nullable List<? extends String> names) {
        ArrayList<Class<? extends Screen>> blacklist = new ArrayList<>();
        if (names != null) {
            for (String s : names) {
                if (StringUtils.isEmpty(s)) {
                    continue;
                }
                try {
                    Class<?> clazz = Class.forName(s, false, ModernUIMod.class.getClassLoader());
                    blacklist.add((Class<? extends Screen>) clazz);
                } catch (ClassNotFoundException e) {
                    ModernUI.LOGGER.warn(ModernUI.MARKER,
                            "Failed to add blur blacklist {}: make sure class name exists", s, e);
                } catch (ClassCastException e) {
                    ModernUI.LOGGER.warn(ModernUI.MARKER,
                            "Failed to add blur blacklist {}: make sure class is a valid subclass of Screen", s, e);
                }
            }
            blacklist.trimToSize();
        }
        mBlacklist = blacklist;
    }

    /**
     * Render tick, should called before rendering things
     */
    public void onRenderTick(long elapsedTimeMillis) {
        if (mFadingIn) {
            float p = Math.min((float) elapsedTimeMillis / sBackgroundDuration, 1.0f);
            if (mBlurring) {
                updateRadius(Math.max(p * sBlurRadius, 1.0f));
            }
            for (int i = 0; i < 4; i++) {
                mBackgroundColor[i] = ColorEvaluator.evaluate(p, 0, sBackgroundColor[i]);
            }
            if (p == 1.0f) {
                mFadingIn = false;
            }
        }
    }

    private void updateRadius(float radius) {
        mBlurRadius = radius;
        PostEffectProcessor effect = minecraft.gameRenderer.getPostProcessor();
        if (effect == null)
            return;
        List<PostEffectPass> passes = ((AccessPostEffectProcessor) effect).getPasses();
        for (PostEffectPass s : passes) {
            s.getProgram().getUniformByNameOrDummy("Progress").set(radius);
        }
    }

    public void onClientTick() {
        float targetVolumeMultiplier;
        if (minecraft.isWindowFocused()) {
            targetVolumeMultiplier = 1;
        } else if (sMasterVolumeMinimized < sMasterVolumeInactive &&
                GLFW.glfwGetWindowAttrib(minecraft.getWindow().getHandle(), GLFW.GLFW_ICONIFIED) != 0) {
            targetVolumeMultiplier = sMasterVolumeMinimized;
        } else {
            targetVolumeMultiplier = sMasterVolumeInactive;
        }
        if (mVolumeMultiplier != targetVolumeMultiplier) {
            // fade down is slower, 1 second = 20 ticks
            if (mVolumeMultiplier < targetVolumeMultiplier) {
                mVolumeMultiplier = Math.min(
                        mVolumeMultiplier + 0.5f,
                        targetVolumeMultiplier
                );
            } else {
                mVolumeMultiplier = Math.max(
                        mVolumeMultiplier - 0.05f,
                        targetVolumeMultiplier
                );
            }
            float volume = minecraft.options.getSoundVolume(SoundCategory.MASTER);
            minecraft.getSoundManager().updateSoundVolume(SoundCategory.MASTER, volume * mVolumeMultiplier);
        }
    }

    // INTERNAL HOOK
    public void drawScreenBackground(@Nonnull DrawContext gr, int x1, int y1, int x2, int y2) {
        VertexConsumer consumer = gr.getVertexConsumers().getBuffer(RenderLayer.getGui());
        Matrix4f pose = gr.getMatrices().peek().getPositionMatrix();
        int z = 0;
        if (minecraft.world == null) {
            consumer.vertex(pose, x2, y1, z)
                    .color(25, 25, 25, 255)
                .next();
            consumer.vertex(pose, x1, y1, z)
                    .color(25, 25, 25, 255)
                .next();
            consumer.vertex(pose, x1, y2, z)
                    .color(25, 25, 25, 255)
                .next();
            consumer.vertex(pose, x2, y2, z)
                    .color(25, 25, 25, 255)
                .next();
        } else {
            if (mBlurring && !mBlurLoaded) {
                MuiModApi.get().loadEffect(minecraft.gameRenderer, BLUR_POST_EFFECT);
                updateRadius(mBlurRadius);
                mBlurLoaded = true;
            }

            int color = mBackgroundColor[1];
            consumer.vertex(pose, x2, y1, z)
                    .color(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff, color >>> 24)
                .next();
            color = mBackgroundColor[0];
            consumer.vertex(pose, x1, y1, z)
                    .color(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff, color >>> 24)
                .next();
            color = mBackgroundColor[3];
            consumer.vertex(pose, x1, y2, z)
                    .color(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff, color >>> 24)
                .next();
            color = mBackgroundColor[2];
            consumer.vertex(pose, x2, y2, z)
                    .color(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff, color >>> 24)
                .next();
        }
        gr.draw();
    }
}
