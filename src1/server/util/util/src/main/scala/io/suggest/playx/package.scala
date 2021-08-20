package io.suggest

import play.api.Mode

package object playx {

  implicit final class AppModeExt( private val mode: Mode ) extends AnyVal {
    def isDev   = mode == Mode.Dev
    def isProd  = mode == Mode.Prod
    def isTest  = mode == Mode.Test
  }

}
