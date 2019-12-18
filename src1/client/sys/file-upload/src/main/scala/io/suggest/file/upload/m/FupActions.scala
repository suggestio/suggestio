package io.suggest.file.upload.m

import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 12:26
  * Description: Экшены для формы аплоада файла.
  */
sealed trait IFupAction extends DAction

case class FileChanged(  ) extends IFupAction
