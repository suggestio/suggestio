package controllers.sc

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.18 15:04
  * Description: Единое API для выдачи: любые запросы и их комбинации в рамках только одного экшена.
  * Это позволяет запрашивать, например, focused и grid карточки одновременно только одним запросом.
  */
trait ScUapi
  extends ScIndex
  with ScAdsTile
  with ScIndexAdOpen
{



}
