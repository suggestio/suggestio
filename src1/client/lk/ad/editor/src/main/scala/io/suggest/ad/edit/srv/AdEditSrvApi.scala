package io.suggest.ad.edit.srv

import diode.ModelRO
import io.suggest.ad.edit.m.MAdEditFormConf
import io.suggest.file.up.{MFile4UpProps, MUploadResp}
import io.suggest.routes.routes
import io.suggest.up.UploadUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 22:22
  * Description: Интерфейс API сервера для взаимодействия в контексте редактирования рекламной карточки.
  */
trait IAdEditSrvApi {

  /** Шаг 1 подготовки к аплоаду файла (картинки, обычно) в контексте карточки.
    *
    * @param file4UpProps Данные файла, готовящегося к заливке.
    * @return Фьючерс с ответом сервера.
    */
  def prepareUpload(file4UpProps: MFile4UpProps): Future[MUploadResp]

}


/** Реализация [[IAdEditSrvApi]] поверх HTTP. */
class AdEditSrvApiHttp( confRO: ModelRO[MAdEditFormConf] ) extends IAdEditSrvApi {

  import JsRoutes_Controllers_LkAdEdit._


  override def prepareUpload(file4UpProps: MFile4UpProps): Future[MUploadResp] = {
    val conf = confRO.value
    val route = routes.controllers.LkAdEdit.prepareImgUpload(
      conf.adId.orNull,
      nodeId = conf.adId.fold(conf.producerId)(_ => null)
    )
    UploadUtil.prepareUploadXhr( route, file4UpProps )
  }

}
