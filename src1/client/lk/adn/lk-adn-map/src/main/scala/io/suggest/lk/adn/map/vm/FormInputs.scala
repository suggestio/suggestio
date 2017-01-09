package io.suggest.lk.adn.map.vm

import io.suggest.maps.vm.inp.{InputsHelpers, MapStateInputs}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.16 16:59
  * Description: Поддержка точечных инпутов геоформы.
  */
trait MapPinInputs extends InputsHelpers {

  object pin extends SetLatLon {
    override lazy val lat = InpPinLat.find()
    override lazy val lon = InpPinLon.find()
  }

}


/** Над-vm инпутов гео-формы. */
class FormInputs
  extends MapPinInputs
  with MapStateInputs
