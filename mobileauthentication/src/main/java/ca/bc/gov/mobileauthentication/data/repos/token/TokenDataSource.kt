package ca.bc.gov.mobileauthentication.data.repos.token

import ca.bc.gov.mobileauthentication.data.models.Token
import io.reactivex.Observable
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
interface TokenDataSource {

    fun getToken(authResponse: AuthorizationResponse? = null): Observable<Token>

    fun saveToken(token: Token): Observable<Token>

    fun refreshToken(token: Token): Observable<Token>

    fun deleteToken(): Observable<Boolean>

}