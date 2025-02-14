package ca.bc.gov.mobileauthentication.screens.redirect

import android.content.Intent
import ca.bc.gov.mobileauthentication.data.repos.token.TokenRepo
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import net.openid.appauth.AuthorizationResponse

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
class RedirectPresenter(
        private val view: RedirectContract.View,
        private val authEndpoint: String,
        private val redirectUri: String,
        private val clientId: String,
        private val responseType: String,
        private val hint: String,
        private val tokenRepo: TokenRepo,
        private val gson: Gson
): RedirectContract.Presenter {

    private val disposables = CompositeDisposable()

    init {
        view.presenter = this
    }

    override fun subscribe() {
        setViewLoginMode()
        view.setUpLoginListener()
    }

    override fun dispose() {
        disposables.dispose()
    }

    override fun loginClicked() {
        if (!view.loading) view.loadWithChrome(buildAuthUrl())
    }

    // Auth url
    fun buildAuthUrl(): String =
            "$authEndpoint?response_type=$responseType&client_id=$clientId&redirect_uri=$redirectUri&kc_idp_hint=$hint"

    // Redirect
    override fun redirectReceived(redirectIntent: Intent?) {
        if (redirectIntent == null)
            return

        val response = AuthorizationResponse.fromIntent(redirectIntent) ?: return

        if (response.authorizationCode == null || response.authorizationCode!!.isEmpty())
            return

        getToken(response)
    }

    /**
     * Gets token remotely using Authorization Code and saves locally
     */
    fun getToken(authResponse: AuthorizationResponse) {
        tokenRepo.getToken(authResponse)
                .firstOrError()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    setViewLoadingMode()
                }.subscribeBy(
                onError = {
                    setViewLoginMode()

                    view.setResultError(it.message ?: "Error logging in")
                    view.finish()
                },
                onSuccess = { token ->
                    setViewLoginMode()

                    val tokenJson = gson.toJson(token)
                    view.setResultSuccess(tokenJson)
                    view.finish()
                }
        ).addTo(disposables)
    }

    fun setViewLoginMode() {
        view.loading = false
        view.hideLoading()
        view.setLoginTextLogin()
    }

    fun setViewLoadingMode() {
        view.loading = true
        view.showLoading()
        view.setLoginTextLoggingIn()
    }
}