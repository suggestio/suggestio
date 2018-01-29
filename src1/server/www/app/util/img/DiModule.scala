package util.img

import models.im.make.IImgMaker
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}
import util.blocks.BlkImgMaker
import util.showcase.ScWideMaker

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.04.16 19:29
  * Description: Настройка инжекции различной утили для img-нужд.
  */
class DiModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      // Регистрируем релаизации IMaker'ов под разными именами.
      bind( classOf[IImgMaker] )
        .qualifiedWith( "blk" )
        .to( classOf[BlkImgMaker] ),

      bind( classOf[IImgMaker] )
        .qualifiedWith( "scWide" )
        .to( classOf[ScWideMaker] ),

      bind( classOf[IImgMaker] )
        .qualifiedWith( "strictWide" )
        .to( classOf[StrictWideMaker] )
    )
  }

}
