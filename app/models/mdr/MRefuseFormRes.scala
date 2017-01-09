package models.mdr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.03.16 13:43
  * Description: Модель результата биндинга формы отказа в размещении.
  * @param reason Рукописная причина отказа.
  * @param modeOpt Режим отказа, если есть, описывающий какие-то дополнительные действия при отказе.
  */
case class MRefuseFormRes(
  reason    : String,
  modeOpt   : Option[MRefuseMode]
) {

  def mode = modeOpt.getOrElse( MRefuseModes.default )

}
