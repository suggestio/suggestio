/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package securesocial.controllers

import play.api.mvc._
import securesocial.core._
import securesocial.util.LoggerImpl

object ProviderControllerHelper extends LoggerImpl {

  /**
   * Remove securesocial keys from session data.
   * @param s Current session.
   * @return Cleaned session.
   */
  def cleanupSession(s: Session): Session = {
    val filteredKeys = Set(
      SecureSocial.OriginalUrlKey,
      IdentityProvider.SessionId,
      OAuth1Provider.CacheKey
    )
    s.copy(
      data = s.data.filterKeys { k =>
        !filteredKeys.contains(k)
      }
    )
  }

}
