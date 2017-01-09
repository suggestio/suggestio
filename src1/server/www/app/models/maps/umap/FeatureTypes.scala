package models.maps.umap

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsValT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.03.16 15:16
  * Description:
  */
object FeatureTypes extends EnumMaybeWithName with EnumJsonReadsValT {

  protected [this] class Val(val name: String) extends super.Val(name)

  override type T = Val

  val Feature: T = new Val("Feature")

  val FeatureCollection: T = new Val("FeatureCollection")

}
