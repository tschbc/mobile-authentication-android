package ca.bc.gov.mobileauthentication.screens.redirect

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import ca.bc.gov.mobileauthentication.MobileAuthenticationClient
import ca.bc.gov.mobileauthentication.R
import ca.bc.gov.mobileauthentication.common.Constants
import ca.bc.gov.mobileauthentication.data.AppAuthApi
import ca.bc.gov.mobileauthentication.di.Injection
import ca.bc.gov.mobileauthentication.di.InjectionUtils
import kotlinx.android.synthetic.main.activity_login.*

/**
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by Aidan Laing on 2017-12-12.
 *
 */
class RedirectActivity : AppCompatActivity(), RedirectContract.View {

    override var presenter: RedirectContract.Presenter? = null

    override var loading: Boolean = false

    private lateinit var appauthApi: AppAuthApi

    companion object {
        const val BASE_URL = "BASE_URL"
        const val REALM_NAME = "REALM_NAME"
        const val AUTH_ENDPOINT = "AUTH_ENDPOINT"
        const val REDIRECT_URI = "REDIRECT_URI"
        const val CLIENT_ID = "CLIENT_ID"
        const val HINT = "HINT"
    }

    // Life cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val baseUrl: String? = intent.getStringExtra(BASE_URL)
        val realmName: String? = intent.getStringExtra(REALM_NAME)
        val authEndpoint: String? = intent.getStringExtra(AUTH_ENDPOINT)
        val redirectUri: String? = intent.getStringExtra(REDIRECT_URI)
        val clientId: String? = intent.getStringExtra(CLIENT_ID)
        val hint: String? = intent.getStringExtra(HINT)

        // Checking for required params
        if (baseUrl == null) {
            showToastAndFinish(getString(R.string.error_missing_base_url))
            return
        }

        if (realmName == null) {
            showToastAndFinish(getString(R.string.error_missing_realm_name))
            return
        }

        if (authEndpoint == null) {
            showToastAndFinish(getString(R.string.error_missing_auth_endpoint))
            return
        }

        if (redirectUri == null) {
            showToastAndFinish(getString(R.string.error_missing_redirect_uri))
            return
        }

        if (clientId == null) {
            showToastAndFinish(getString(R.string.error_missing_client_id))
            return
        }

        if (hint == null) {
            showToastAndFinish(getString(R.string.error_missing_hint))
            return
        }

        // Building presenter params
        val responseType = Constants.RESPONSE_TYPE_CODE

        appauthApi = AppAuthApi(this, baseUrl, realmName, authEndpoint, redirectUri, clientId, hint)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val tokenRepo = InjectionUtils.getTokenRepo(appauthApi, sharedPreferences)

        val gson = Injection.provideGson()

        RedirectPresenter(
                this, authEndpoint, redirectUri, clientId, responseType, hint, tokenRepo, gson)

        presenter?.subscribe()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.dispose()
    }

    // Toasts
    private fun showToastAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        setResultError(message)
        finish()
    }

    // Deep link triggered
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkIntentForRedirect(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MobileAuthenticationClient.APPAUTH_REQUEST_CODE)
            checkIntentForRedirect(data)
    }

    private fun checkIntentForRedirect(intent: Intent?) {
        presenter?.redirectReceived(intent)
    }

    // Loading
    override fun showLoading() {
        progressBar.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    // Login
    override fun setUpLoginListener() {
        loginTv.setOnClickListener {
            presenter?.loginClicked()
        }
    }

    override fun setLoginTextLogin() {
        loginTv.setText(R.string.login)
    }

    override fun setLoginTextLoggingIn() {
        loginTv.setText(R.string.logging_in)
    }

    /**
     * Goes to Chrome custom tab
     */
    override fun loadWithChrome(url: String) {
        val colorScheme = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build()

        val customTabsIntent = CustomTabsIntent.Builder()
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setDefaultColorSchemeParams(colorScheme)
                .setShowTitle(true)
                .build()

        val authIntent = appauthApi.getAuthRequestIntent(customTabsIntent)
        startActivityForResult(authIntent, MobileAuthenticationClient.APPAUTH_REQUEST_CODE)
    }

    // Results
    override fun setResultError(errorMessage: String) {
        val data = Intent()
        data.putExtra(MobileAuthenticationClient.SUCCESS, false)
        data.putExtra(MobileAuthenticationClient.ERROR_MESSAGE, errorMessage)
        setResult(Activity.RESULT_OK, data)
    }

    override fun setResultSuccess(tokenJson: String) {
        val data = Intent()
        data.putExtra(MobileAuthenticationClient.SUCCESS, true)
        data.putExtra(MobileAuthenticationClient.TOKEN_JSON, tokenJson)
        setResult(Activity.RESULT_OK, data)
    }

    override fun onBackPressed() {
        setResultError("Login cancelled by user")
        super.onBackPressed()
    }
}
