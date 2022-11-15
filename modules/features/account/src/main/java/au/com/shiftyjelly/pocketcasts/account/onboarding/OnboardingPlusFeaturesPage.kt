package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.R
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import kotlinx.coroutines.delay
import java.lang.Long.max
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val background = Color(0xFF121212)

@Composable
fun OnboardingPlusFeaturesPage(
    onShown: () -> Unit,
    onUpgradePressed: () -> Unit,
    onNotNowPressed: () -> Unit,
    onBackPressed: () -> Unit,
) {

    val viewModel = hiltViewModel<OnboardingPlusFeaturesViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { onShown() }
    BackHandler { onBackPressed() }

    Box(
        Modifier
            .verticalScroll(rememberScrollState())
            .background(background)
    ) {
        Background()

        Column {

            Spacer(Modifier.height(8.dp))
            NavigationIconButton(
                onNavigationClick = onBackPressed,
                iconColor = Color.White,
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp)
            )

            Spacer(Modifier.height(12.dp))

            Column {

                IconRow(Modifier.padding(horizontal = 24.dp))

                Spacer(Modifier.height(36.dp))

                TextH10(
                    text = stringResource(LR.string.onboarding_upgrade_everything_you_love_about_pocket_casts_plus),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(Modifier.height(12.dp))

                TextP40(
                    text = stringResource(LR.string.onboarding_upgrade_exclusive_features_and_options),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(Modifier.height(58.dp))

                FeatureRow(scrollAutomatically = state.scrollAutomatically)

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(36.dp))

                PlusRowButton(
                    text = stringResource(LR.string.onboarding_upgrade_unlock_all_features),
                    onClick = onUpgradePressed,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(Modifier.height(16.dp))

                PlusOutlinedRowButton(
                    text = stringResource(LR.string.not_now),
                    onClick = onNotNowPressed,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun IconRow(modifier: Modifier = Modifier) {
    Row(modifier) {
        Icon(
            painter = painterResource(R.drawable.pocket_casts_white),
            contentDescription = null,
            tint = Color.White,
        )

        Spacer(Modifier.width(8.dp))

        Icon(
            painter = painterResource(R.drawable.plus_bw),
            contentDescription = null,
            tint = Color.White,
        )
    }
}

@Composable
private fun FeatureRow(scrollAutomatically: Boolean) {

    // Not using rememberLazyListState() because we want to reset
    // the scroll state on orientation changes so that the hardcoded column
    // is redisplayed, which insures the height is correctly calculated. For that
    // reason, we want to use remember, not rememberSaveable.
    val state = remember { LazyListState() }

    val localConfiguration = LocalConfiguration.current
    LaunchedEffect(scrollAutomatically) {
        if (scrollAutomatically) {
            // This seems to get a good scroll speed across multiple devices
            val scrollDelay = max(1L, (1000L - localConfiguration.densityDpi) / 125)
            autoScroll(scrollDelay, state)
        }
    }
    var height by remember { mutableStateOf<Dp?>(null) }
    val density = LocalDensity.current
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = !scrollAutomatically,
        modifier = height?.let { Modifier.height(it) } ?: Modifier
    ) {

        item {
            // This row is needed to determine the height of the tallest list item,
            // which is then used to keep the size of the LazyRow constant regardless
            // of the height of the items it is displaying at a given time. If
            // IntrinsicSize.Max could work with LazyRows, we wouldn't need to do this.
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .height(IntrinsicSize.Max)
                    .onSizeChanged {
                        height = density.run { it.height.toDp() }
                    }
            ) {
                FeatureItemContent.values().forEach {
                    FeatureItem(it)
                }
            }
        }

        if (scrollAutomatically) {
            val contentArray = FeatureItemContent.values()
            items(
                count = 200, // Big number that users should never get to
                contentType = { it % contentArray.size },
            ) { n ->
                val content = contentArray[n % contentArray.size]
                FeatureItem(content)
            }
        }
    }
}

@Composable
private fun FeatureItem(
    content: FeatureItemContent,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .border(
                width = 1.dp,
                color = Color(0xFF383839),
                shape = shape,
            )
            .background(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF2A2A2B),
                    1f to Color(0xFF252525),
                ),
                shape = shape,
            )
            .width(156.dp)
            .fillMaxHeight()
            .padding(all = 16.dp)
    ) {

        Icon(
            painter = painterResource(content.image),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        TextH40(
            text = stringResource(content.title),
            color = Color.White,
        )
        Spacer(Modifier.height(4.dp))
        TextP60(
            text = stringResource(content.text),
            color = Color.White,
            modifier = Modifier.alpha(0.72f),
        )
    }
}

enum class FeatureItemContent(
    @DrawableRes val image: Int,
    @StringRes val title: Int,
    @StringRes val text: Int,
) {
    DesktopApps(
        image = IR.drawable.desktop_apps,
        title = LR.string.onboarding_plus_feature_desktop_apps_title,
        text = LR.string.onboarding_plus_feature_desktop_apps_text,
    ),
    Folders(
        image = IR.drawable.ic_folder,
        title = LR.string.onboarding_plus_feature_folders_title,
        text = LR.string.onboarding_plus_feature_folders_text,
    ),
    CloudStorage(
        image = IR.drawable.ic_cloud,
        title = LR.string.onboarding_plus_feature_cloud_storage_title,
        text = LR.string.onboarding_plus_feature_cloud_storage_text,
    ),
    HideAds(
        image = IR.drawable.ads_disabled,
        title = LR.string.onboarding_plus_feature_hide_ads_title,
        text = LR.string.onboarding_plus_feature_hide_ads_text,
    ),
    ThemesIcons(
        image = IR.drawable.whatsnew_theme,
        title = LR.string.onboarding_plus_feature_themes_icons_title,
        text = LR.string.onboarding_plus_feature_themes_icons_text,
    ),
}

@Composable
private fun Background() {

    // Blur only works on Android >=12
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

        val height = LocalConfiguration.current.screenHeightDp
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp) // not using fillMaxHeight because that caused issues in vertically scrollable views
                .blur(150.dp)
        ) {

            // Background
            drawRect(background)

            drawCircle(
                color = Color(0xFFFFD845),
                radius = size.width * .5f,
                center = Offset(size.width * .05f, size.height * .05f),
            )

            drawCircle(
                color = Color(0xFFFFB626),
                radius = size.width * .35f,
                center = Offset(size.width * .95f, size.height * .18f),
                alpha = 0.8f,
            )

            // Overlay
            drawRect(Color(0xFF121212), alpha = 0.28f)
        }
    } else {
        Column(
            Modifier
                .background(background)
                .fillMaxSize()
        ) {
            Image(
                painterResource(R.drawable.upgrade_background_glows),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private val plusGradientBrush = Brush.horizontalGradient(
    0f to Color(0xFFFED745),
    1f to Color(0xFFFEB525),
)

@Composable
private fun PlusRowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(all = 0.dp), // Remove content padding
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(plusGradientBrush)
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(6.dp)
                    // add extra 8.dp extra padding to offset removal of button padding (see ButtonDefaults.ButtonVerticalPadding)
                    .padding(8.dp)
                    .align(Alignment.Center),
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun PlusOutlinedRowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, plusGradientBrush),
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Text(
                text = text,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(6.dp)
                    .align(Alignment.Center)
                    .textBrush(plusGradientBrush)
            )
        }
    }
}

// Based on https://stackoverflow.com/a/71344813/1910286
private tailrec suspend fun autoScroll(
    scrollDelay: Long,
    lazyListState: LazyListState,
) {
    val scrollAmount = lazyListState.scrollBy(1f)
    if (scrollAmount == 0f) {
        // If we can't scroll, we're at the end, so jump to the beginning.
        // This will be an abrupt jump, but users shouldn't really ever be
        // getting to the end of the list, so it should be very rare.
        lazyListState.scrollToItem(0)
    }
    delay(scrollDelay)
    autoScroll(scrollDelay, lazyListState)
}

// From https://stackoverflow.com/a/71376469/1910286
private fun Modifier.textBrush(brush: Brush) = this
    .graphicsLayer(alpha = 0.99f)
    .drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(brush, blendMode = BlendMode.SrcAtop)
        }
    }

@Preview
@Composable
private fun OnboardingPlusFeaturesPreview() {
    OnboardingPlusFeaturesPage(
        onShown = {},
        onBackPressed = {},
        onUpgradePressed = {},
        onNotNowPressed = {},
    )
}
