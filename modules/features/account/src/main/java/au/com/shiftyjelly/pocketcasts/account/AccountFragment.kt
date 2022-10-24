package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import au.com.shiftyjelly.pocketcasts.account.viewmodel.AccountFragmentViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.GradientIcon
import au.com.shiftyjelly.pocketcasts.compose.components.GradientIconData
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.account.R as AR
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AccountFragment : BaseFragment() {
    companion object {
        private const val BUTTON = "button"
        private const val SIGN_IN = "sign_in"
        private const val CREATE_ACCOUNT = "create_account"
        fun newInstance() = AccountFragment()
    }

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private val viewModel: AccountFragmentViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    SignInOrCreatePage(
                        onCreateAccountClick = { createAccountClicked() },
                        onSignInClick = { signInClicked() },
                        onGoogleSignInClick = { googleOneTapSignInLauncher, googleLegacySignInLauncher ->
                            viewModel.beginSignInGoogleOneTap(
                                onGoogleOneTapSignInIntent = { intent -> googleOneTapSignInLauncher.launch(intent) },
                                onGoogleLegacySignInIntent = { intent -> googleLegacySignInLauncher.launch(intent) },
                            )
                        },
                        onGoogleOneTapSignInResult = { result, googleLegacySignInLauncher ->
                            viewModel.onGoogleOneTapSignInResult(
                                result = result,
                                onGoogleLegacySignInIntent = { intent -> googleLegacySignInLauncher.launch(intent) }
                            )
                        },
                        onGoogleLegacySignInResult = { result ->
                            viewModel.onGoogleLegacySignInResult(result)
                        },
                        showContinueWithGoogleButton = viewModel.showContinueWithGoogleButton
                    )
                }
            }
        }
    }

    private fun closeClicked() {
        activity?.finish()
    }

    private fun createAccountClicked() {
        val view = view ?: return
        val navController = view.findNavController()
        analyticsTracker.track(AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED, mapOf(BUTTON to CREATE_ACCOUNT))
        FirebaseAnalyticsTracker.createAccountClicked()
        if (navController.currentDestination?.id == AR.id.accountFragment) {
            if (Util.isCarUiMode(view.context)) { // We can't sign up to plus on cars so skip that step
                navController.navigate(AR.id.action_accountFragment_to_createEmailFragment)
            } else {
                navController.navigate(AR.id.action_accountFragment_to_createAccountFragment)
            }
        }
    }

    private fun signInClicked() {
        val view = view ?: return
        val navController = view.findNavController()
        analyticsTracker.track(AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED, mapOf(BUTTON to SIGN_IN))
        FirebaseAnalyticsTracker.signInAccountClicked()
        if (navController.currentDestination?.id == AR.id.accountFragment) {
            navController.navigate(AR.id.action_accountFragment_to_signInFragment)
        }
    }

    @Composable
    private fun SignInOrCreatePage(
        onCreateAccountClick: () -> Unit,
        onSignInClick: () -> Unit,
        onGoogleSignInClick: (ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>, ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>) -> Unit,
        onGoogleOneTapSignInResult: (ActivityResult, ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>) -> Unit,
        onGoogleLegacySignInResult: (ActivityResult) -> Unit,
        modifier: Modifier = Modifier,
        showContinueWithGoogleButton: Boolean
    ) {
        val googleLegacySignInLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            onGoogleLegacySignInResult(result)
        }
        val googleOneTapSignInLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            onGoogleOneTapSignInResult(result, googleLegacySignInLauncher)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .background(MaterialTheme.theme.colors.secondaryUi01)
                .fillMaxHeight()
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            ThemedTopAppBar(
                navigationButton = NavigationButton.Close,
                onNavigationClick = { closeClicked() }
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(all = 16.dp)
                    .widthIn(0.dp, 600.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                CreateNewAccountIcon()
                Spacer(Modifier.height(24.dp))
                TextH20(stringResource(LR.string.profile_sign_in_or_create_account))
                Spacer(Modifier.height(8.dp))
                TextP30(
                    text = stringResource(LR.string.profile_save_your_podcasts),
                    color = MaterialTheme.theme.colors.primaryText02,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
                Spacer(Modifier.height(56.dp))
                if (showContinueWithGoogleButton) {
                    GoogleSignInButton(onClick = { onGoogleSignInClick(googleOneTapSignInLauncher, googleLegacySignInLauncher) })
                    Spacer(Modifier.height(16.dp))
                }
                CreateAccountButton(onCreateAccountClick)
                Spacer(Modifier.height(16.dp))
                SignInButton(onSignInClick)
            }
        }
    }

    @Composable
    private fun CreateNewAccountIcon() {
        GradientIcon(
            icon = GradientIconData(
                res = AR.drawable.ic_create_new_account,
                colors = listOf(
                    MaterialTheme.theme.colors.gradient02A,
                    MaterialTheme.theme.colors.gradient02E
                )
            ),
            width = 171.dp,
            height = 160.dp
        )
    }

    private val googleButtonBorder: BorderStroke
        @Composable
        get() = BorderStroke(width = 2.dp, color = MaterialTheme.theme.colors.primaryInteractive03)

    @Composable
    private fun GoogleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
        // TODO maybe move this to another file
        Row(
            modifier = modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                border = googleButtonBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.CenterStart) {
                    Image(
                        painter = painterResource(IR.drawable.google_logo),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(LR.string.continue_with_google),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.theme.colors.primaryText01,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(horizontal = 30.dp, vertical = 6.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    @Composable
    private fun CreateAccountButton(onClick: () -> Unit) {
        RowButton(
            text = stringResource(LR.string.create_account),
            onClick = onClick,
            includePadding = false
        )
    }

    @Composable
    private fun SignInButton(onClick: () -> Unit) {
        RowTextButton(
            text = stringResource(LR.string.sign_in),
            onClick = onClick
        )
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val viewModel: AccountFragmentViewModel by viewModels()
//        viewModel.signInState.observeOnce(
//            viewLifecycleOwner,
//            Observer {
//                val binding = realBinding ?: return@Observer
//
//                if (it is SignInState.SignedIn) {
//                    binding.btnSignIn.isVisible = false
//                    binding.lblSignIn.text = getString(LR.string.profile_alreadysignedin)
//                    binding.lblSaveYourPodcasts.text = getString(LR.string.profile_alreadysignedindescription)
//                    binding.imgCreateAccount.setup(view.context.getThemeTintedDrawable(IR.drawable.ic_alert_small, UR.attr.support_05))
//                    binding.btnCreate.text = getString(LR.string.done)
//                    binding.btnCreate.setOnClickListener { activity?.finish() }
//                } else {
//                    val res = if (accountViewModel.supporterInstance) LR.string.profile_supporter_description else LR.string.profile_save_your_podcasts
//                    binding.lblSaveYourPodcasts.setText(res)
//                }
//            }
//        )
//
//        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            binding.imgCreateAccount.updateLayoutParams {
//                width = 0
//                height = 0
//            }
//        }
//
//        binding.btnClose?.setOnClickListener {
//            analyticsTracker.track(AnalyticsEvent.SETUP_ACCOUNT_DISMISSED)
//            FirebaseAnalyticsTracker.closeAccountMissingClicked()
//            activity?.finish()
//        }
//
//        binding.btnCreate.setOnClickListener {
//            analyticsTracker.track(AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED, mapOf(BUTTON to CREATE_ACCOUNT))
//            FirebaseAnalyticsTracker.createAccountClicked()
//            if (view.findNavController().currentDestination?.id == R.id.accountFragment) {
//                if (Util.isCarUiMode(view.context) || accountViewModel.supporterInstance) { // We can't sign up to plus on cars so skip that step
//                    view.findNavController().navigate(R.id.action_accountFragment_to_createEmailFragment)
//                } else {
//                    view.findNavController().navigate(R.id.action_accountFragment_to_createAccountFragment)
//                }
//            }
//        }
//
//        binding.btnSignIn.setOnClickListener {
//            analyticsTracker.track(AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED, mapOf(BUTTON to SIGN_IN))
//            FirebaseAnalyticsTracker.signInAccountClicked()
//            if (view.findNavController().currentDestination?.id == R.id.accountFragment) {
//                view.findNavController().navigate(R.id.action_accountFragment_to_signInFragment)
//            }
//        }
//
//        binding.btnGoogle?.setOnClickListener {
//            launchGoogleSignIn()
//        }
//    }
//
//    private val loginResultHandler = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
//        processGoogleSignInResult(result)
//    }
//
//    private fun launchGoogleSignIn() {
//        val viewModel: AccountFragmentViewModel by viewModels()
//        viewModel.launchGoogleSignIn(
//            onSuccess = { pendingIntent ->
//                try {
//                    val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
//                    loginResultHandler.launch(intentSenderRequest)
//                } catch (e: Exception) {
//                    Timber.e(e, "Google sign-in failed to launch account picker.")
//                }
//            },
//            onFailure = { exception ->
//                Timber.e(exception, "Google sign-in failed to start.")
//                // TODO let the user know about the failure
//            }
//        )
//    }
//
//    private fun processGoogleSignInResult(result: ActivityResult) {
//        val activity = activity ?: return
//
//        if (result.resultCode != Activity.RESULT_OK) {
//            Timber.e("Google sign-in failed to return a result.")
//            return
//        }
//
//        val viewModel: AccountFragmentViewModel by viewModels()
//        val data = result.data
//        try {
//            val credential = Identity.getSignInClient(activity).getSignInCredentialFromIntent(data)
//            viewModel.signInWithGoogle(credential)
//        } catch (e: ApiException) {
//            Timber.e(e, "Google sign-in failed as unable to process result.")
//        }
//    }
}
