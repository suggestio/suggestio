package io.suggest.model

import akka.stream.Materializer
import io.suggest.di.{ICurrentActorSystem, IExecutionContext, ISioNotifier}
import io.suggest.playx.{ICurrentAppHelpers, ICurrentConf}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.17 11:14
  * Description: Базовый трейт для контейнера всяких очень общих DI-инстансов.
  */
trait ICommonDiValBase
  extends IExecutionContext
  with ISioNotifier
  with ICurrentConf
  with ICurrentAppHelpers
  with ICurrentActorSystem
{
  implicit val mat                 : Materializer
}
