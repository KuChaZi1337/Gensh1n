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

package dev.undefinedteam.gensh1n.mixins.modernui.ui;

import dev.undefinedteam.modernui.mc.MuiModApi;
import net.minecraft.client.util.SelectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SelectionManager.class)
public class MixinSelectionManager {

    @Redirect(
            method = "moveCursor(IZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Util;moveCursor(Ljava/lang/String;II)I"
            )
    )
    private int onMoveByChars(String value, int cursor, int dir) {
        return MuiModApi.offsetByGrapheme(value, cursor, dir);
    }

    @Redirect(
            method = "delete(I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Util;moveCursor(Ljava/lang/String;II)I"
            )
    )
    private int onRemoveCharsFromCursor(String value, int cursor, int dir) {
        return MuiModApi.offsetByGrapheme(value, cursor, dir);
    }
}
