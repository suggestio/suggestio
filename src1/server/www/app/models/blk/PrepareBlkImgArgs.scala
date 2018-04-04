package models.blk

import io.suggest.model.play.qsb.QueryStringBindableImpl
import models.blk.ed.BimKey_t
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.11.15 12:03
  * Description: URL qs модель для аргументов подготовки изображения под блок.
  */

object PrepareBlkImgArgs {

  def BIM_KEY_FN  = "i"
  def WS_ID_FN    = "w"

  implicit def prepareBlkImgArgsQsb(implicit
                                    bimKeyB  : QueryStringBindable[BimKey_t],
                                    strOptB  : QueryStringBindable[Option[String]]
                                   ): QueryStringBindable[PrepareBlkImgArgs] = {
    new QueryStringBindableImpl[PrepareBlkImgArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, PrepareBlkImgArgs]] = {
        val k = key1F(key)
        for {
          bimKeyEith  <- bimKeyB.bind   (k(BIM_KEY_FN), params)
          wsIdOptEith <- strOptB.bind   (k(WS_ID_FN),   params)
        } yield {
          for {
            bimKey      <- bimKeyEith.right
            wsIdOpt     <- wsIdOptEith.right
          } yield {
            PrepareBlkImgArgs(bimKey, wsIdOpt)
          }
        }
      }

      override def unbind(key: String, value: PrepareBlkImgArgs): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          bimKeyB.unbind    (k(BIM_KEY_FN), value.bimKey),
          strOptB.unbind    (k(WS_ID_FN),   value.wsId)
        )
      }
    }
  }

}


case class PrepareBlkImgArgs(
  bimKey  : BimKey_t,
  wsId    : Option[String]
)
