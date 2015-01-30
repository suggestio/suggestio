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
package securesocial.core.java;

import org.joda.time.DateTime;

/**
 * A token used for reset password and sign up operations
 */
public class Token {
    public String uuid;
    public String email;
    public DateTime creationTime;
    public DateTime expirationTime;
    public boolean isSignUp;

    public boolean isExpired() {
        return expirationTime.isBeforeNow();
    }

    public securesocial.core.providers.MailToken toScala() {
        return securesocial.core.providers.MailToken$.MODULE$.apply(
                uuid, email, creationTime, expirationTime, isSignUp
        );
    }

    public static Token fromScala(securesocial.core.providers.MailToken scalaToken) {
        Token javaToken = new Token();
        javaToken.uuid = scalaToken.uuid();
        javaToken.email = scalaToken.email();
        javaToken.creationTime = scalaToken.creationTime();
        javaToken.expirationTime = scalaToken.expirationTime();
        javaToken.isSignUp = scalaToken.isSignUp();
        return javaToken;
    }
}
