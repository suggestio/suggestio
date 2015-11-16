package models.blk

import models.BlockConf
import models.blk.ed.BimKey_t
import play.api.mvc.QueryStringBindable
import util.qsb.QsbKey1T

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.11.15 12:03
  * Description: URL qs модель для аргументов подготовки изображения под блок.
  */

object PrepareBlkImgArgs {

  def BC_FN       = "b"
  def BIM_KEY_FN  = "i"
  def WS_ID_FN    = "w"

  implicit def qsb(implicit
                   bcB      : QueryStringBindable[BlockConf],
                   bimKeyB  : QueryStringBindable[BimKey_t],
                   strOptB  : QueryStringBindable[Option[String]]
                  ): QueryStringBindable[PrepareBlkImgArgs] = {
    new QueryStringBindable[PrepareBlkImgArgs] with QsbKey1T {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, PrepareBlkImgArgs]] = {
        val k = key1F(key)
        for {
          bcEith      <- bcB.bind       (k(BC_FN),      params)
          bimKeyEith  <- bimKeyB.bind   (k(BIM_KEY_FN), params)
          wsIdOptEith <- strOptB.bind   (k(WS_ID_FN),   params)
        } yield {
          for {
            bc          <- bcEith.right
            bimKey      <- bimKeyEith.right
            wsIdOpt     <- wsIdOptEith.right
          } yield {
            PrepareBlkImgArgs(bc, bimKey, wsIdOpt)
          }
        }
      }

      override def unbind(key: String, value: PrepareBlkImgArgs): String = {
        val k = key1F(key)
        Iterator(
          bcB.unbind        (k(BC_FN),      value.bc),
          bimKeyB.unbind    (k(BIM_KEY_FN), value.bimKey),
          strOptB.unbind    (k(WS_ID_FN),   value.wsId)
        )
          .filter { _.nonEmpty }
          .mkString("&")
      }
    }
  }

}


case class PrepareBlkImgArgs(
  bc      : BlockConf,
  bimKey  : BimKey_t,
  wsId    : Option[String]
)
