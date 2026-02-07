package com.openfuel.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val OpenFuelShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.s),
    small = RoundedCornerShape(Dimens.s),
    medium = RoundedCornerShape(Dimens.sectionRadius),
    large = RoundedCornerShape(Dimens.cardRadius),
    extraLarge = RoundedCornerShape(Dimens.cardRadius),
)
