package com.moodiary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.moodiary.app.R
import com.moodiary.app.ui.theme.MoodiaryColors

/** The design desaturates every photo slightly (`filter: saturate(0.85)`). */
private val Desaturate = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.85f) })

/**
 * One photo. [model] is either an `https://` sample URL or a `content://` URI from the
 * photo picker — Coil resolves both.
 */
@Composable
fun EntryPhoto(model: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = model,
        contentDescription = stringResource(R.string.cd_photo),
        contentScale = ContentScale.Crop,
        colorFilter = Desaturate,
        modifier = modifier
            .clip(ImageShape)
            .background(MoodiaryColors.ImagePlaceholder),
    )
}

/**
 * The photo block of a timeline card:
 *  - one photo  -> full width, 190dp tall
 *  - two photos -> side by side, 130dp tall
 *  - more       -> a two-column grid of 130dp tiles
 *
 * Laid out with plain rows rather than a lazy grid because the card itself already
 * lives inside a `LazyColumn`.
 */
@Composable
fun EntryPhotoBlock(photos: List<String>, modifier: Modifier = Modifier) {
    when {
        photos.isEmpty() -> Unit
        photos.size == 1 -> EntryPhoto(
            model = photos[0],
            modifier = modifier.fillMaxWidth().height(190.dp),
        )
        else -> Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            photos.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { photo ->
                        EntryPhoto(photo, Modifier.weight(1f).height(130.dp))
                    }
                    // Keep a lone trailing photo half-width instead of stretching it.
                    if (row.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Square thumbnail used by the editor grid and the search results. */
@Composable
fun SquarePhoto(model: String, modifier: Modifier = Modifier) {
    EntryPhoto(model, modifier.aspectRatio(1f))
}

/** 46dp thumbnail on a search result row. */
@Composable
fun ThumbnailPhoto(model: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        colorFilter = Desaturate,
        modifier = modifier
            .size(46.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(MoodiaryColors.ImagePlaceholder),
    )
}
