package io.suggest.sjs.common.vm.of

import io.suggest.sjs.common.vm.find.IApplyEl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.01.16 14:40
 * Description: Трейт поддержки подсистемы опционального apply-элементов,
 * опционально подходящих под VM.
 */

/** Базовый трейт для of-трейтов. */
trait OfBase
  extends IApplyEl


/** Типичный общеупотребительный Of. */
trait Of
  extends OfEventTargetNode


