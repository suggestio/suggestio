package com.github.react.dnd.backend.test

import com.github.react.dnd.IDndBackend

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.08.2019 15:23
  * @see [[http://react-dnd.github.io/react-dnd/docs/backends/test]]
  */
@js.native
@JSImport(PACKAGE_NAME, "TestBackend")
object TestBackend extends IDndBackend