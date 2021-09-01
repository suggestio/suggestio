package io.suggest.i18n

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 21:41
  * Description: Пошаренная модель кодов сообщений.
  */
object MsgCodes {

  val `Subnodes` = "Subnodes"
  val `No.subnodes` = "No.subnodes"
  val `N.nodes` = "N.nodes"
  val `N.disabled` = "N.disabled"
  val `Is.enabled` = "Is.enabled"
  val `Name` = "Name"
  val `New.node` = "New.node"
  val `Delete` = "Delete"
  val `Deletion` = "Deletion"
  val `Yes.delete.it` = "Yes.delete.it"
  val `Are.you.sure` = "Are.you.sure"
  val `Comission.0.pct.for.sio` = "Comission.0.pct.for.sio"
  val `Cost` = "Cost"
  val `Show.details` = "Show.details"
  val `Information` = "Information"
  val `N.modules` = "N.modules"
  val `Send` = "Send"
  val `Send.request` = "Send.request"

  val `Add` = "Add"
  val `Error` = "Error"
  val `Something.gone.wrong` = "Something.gone.wrong"
  val `Identifier` = "Identifier"
  val `Cancel` = "Cancel"
  val `Save` = "Save"
  val `Saving` = "Saving"
  val `Saved` = "Saved"
  val `Change` = "Change"
  val `For.example.0` = "For.example.0"
  val `Close` = "Close"
  val `Create` = "Create"
  val `Edit` = "Edit"
  val `Node.id` = "Node.id"
  val `Please.wait` = "Please.wait"
  val `Please.try.again.later` = "Please.try.again.later"
  val `Example.id.0` = "Example.id.0"

  val `Adv.tariff` = "Adv.tariff"
  val `Inherited` = "Inherited"
  val `Set.manually` = "Set.manually"
  val `_per_.day` = "_per_.day"

  val `Tariff.rate.of.0` = "Tariff.rate.of.0"

  val `Server.request.in.progress.wait` = "Server.request.in.progress.wait"

  val `Beacon.name.example` = "Beacon.name.example"
  val `Wifi.router.name.example` = "Wifi.router.name.example"
  val `Type.new.name.for.beacon.0` = "Type.new.name.for.beacon.0"

  def `ofMonth.N.`(month_1_12: Int) = "ofMonth.N." + month_1_12
  def `DayOfWeek.N.`(dow_1_7: Int) = "DayOfWeek.N." + dow_1_7
  def `DayOfW.N.`(dow_1_7: Int) = "DayOfW.N." + dow_1_7
  def `dayOfW.N.`(dow_1_7: Int) = "dayOfW.N." + dow_1_7

  val `Yes` = "Yes"
  val `No` = "No"

  val `n.km._kilometers` = "n.km._kilometers"
  val `n.m._meters` = "n.m._meters"
  val `n.cm._centimeters` = "n.cm._centimeters"
  val `Radius` = "Radius"
  val `in.radius.of.0.from.1` = "in.radius.of.0.from.1"
  val `in.radius.of.0` = "in.radius.of.0"
  val `_from.you` = "_from.you"

  val `Adv.on.main.screen` = "Adv.on.main.screen"
  val `Adv.in.tag` = "Adv.in.tag"
  val `Coverage.area` = "Coverage.area"
  val `Ad.area.modules.count` = "Ad.area.modules.count"
  val `Tag` = "Tag"
  val `Date` = "Date"
  def `Date.suffixed`(suffix: String) = `Date` + "." + suffix
  val `Date.choosing` = "Date.choosing"
  val `Price` = "Price"
  val `Unable.to.initialize.map` = "Unable.to.initialize.map"
  val `Publish.node.on.adv.map` = "Publish.node.on.adv.map"
  val `Users.geo.location.capturing` = "Users.geo.location.capturing"
  val `You.can.place.adn.node.on.map.below` = "You.can.place.adn.node.on.map.below"
  val `Your.sc.will.be.opened.auto.when.user.geolocated.inside.circle` = "Your.sc.will.be.opened.auto.when.user.geolocated.inside.circle"

  val `Add.tags` = "Add.tags"

  val `Adv.on.map` = "Adv.on.map"
  val `Adv.on.map.hint` = "Adv.on.map.hint"

  val `Read.more` = "Read.more"
  val `Adv.geo.form.descr1` = "Adv.geo.form.descr1"
  val `User.located.inside.are.or.location.will.see.your.offer` = "User.located.inside.are.or.location.will.see.your.offer"
  val `Good.to.know.that` = "Good.to.know.that"
  val `Geo.circle.does.not.touch.any.nodes` = "Geo.circle.does.not.touch.any.nodes"
  val `Nodes.can.contain.subnodes.sublocations.routers.tvs.beacons` = "Nodes.can.contain.subnodes.sublocations.routers.tvs.beacons"
  val `Adv.geo.form.descr.price` = "Adv.geo.form.descr.price"

  val `Support.request` = "Support.request"
  val `Company.about` = "Company.about"
  val `Support.service` = "Support.service"
  val `Describe.problem` = "Describe.problem"
  val `Or.contact.us.via.email` = "Or.contact.us.via.email"

  val `OGRN` = "OGRN"
  val `Organization.name` = "Organization.name"
  val `_Cbca.name.full_` = "_Cbca.name.full_"
  val `INN` = "INN"
  val `KPP` = "KPP"
  val `Bank` = "Bank"
  val `_Cbca.bank.name_` = "_Cbca.bank.name_"
  val `Bank.BIK` = "Bank.BIK"
  val `Checking.account` = "Checking.account"
  val `Correspondent.account` = "Correspondent.account"
  val `Legal.address` = "Legal.address"
  val `_Cbca.address.legal_` = "_Cbca.address.legal_"
  val `Mail.address` = "Mail.address"
  val `_Cbca.address.mail_` = "_Cbca.address.mail_"

  val `Your.name` = "Your.name"
  val `Phone` = "Phone"

  val `Adv.for.free.without.moderation` = "Adv.for.free.without.moderation"

  val `Yandex.Kassa` = "Yandex.Kassa"
  val `Order.price` = "Order.price"
  val `Minimal.payment` = "Minimal.payment"
  val `Order.number` = "Order.number"
  val `Payment.method` = "Payment.method"
  val `Total.to.pay` = "Total.to.pay"
  val `Pay` = "Pay"
  val `Total.amount._money` = "Total.amount._money"
  val `_N` = "_N"
  val `Order.0.is.paid` = "Order.0.is.paid"
  val `Order.N` = "Order.N"
  val `We.received.payment.for.order.0` = "We.received.payment.for.order.0"
  val `Thank.you.for.using.our.service` = "Thank.you.for.using.our.service"
  val `Order.details` = "Order.details"

  val `Minimal.module` = "Minimal.module"
  val `scheme.left` = "scheme.left"
  val `Current.ad` = "Current.ad"
  val `Agreement.btw.CBCA.and.node.tariffs.for.year` = "Agreement.btw.CBCA.and.node.tariffs.for.year"

  val `Town` = "Town"
  val `Address` = "Address"
  val `Site` = "Site"
  val `Info.about.prods.and.svcs` = "Info.about.prods.and.svcs"
  val `Daily.people.traffic` = "Daily.people.traffic"
  val `Audience.descr` = "Audience.descr"

  val `Hello` = "Hello"
  val `Hello.0` = "Hello.0"
  val `Suggest.io` = "Suggest.io"
  val `iSuggest` = "iSuggest"
  val `All.rights.reserved` = "All.rights.reserved"
  val `day24h` = "day24h"

  val `Main.screen` = "Main.screen"
  val `GeoTag` = "GeoTag"
  val `_adv.Online.now` = "_adv.Online.now"

  val `Multi` = "Multi"

  val `Switch.node` = "Switch.node"

  val `Today` = "Today"

  val `locale.momentjs` = "locale.momentjs"
  val `Advertising.period` = "Advertising.period"


  val `__This.lang.name__` = "__This.lang.name__"

  // Эти коды без messages используются. Вполне вероятно, что в будущем у них появится локализация по языкам.
  val `left` = "left"
  val `right` = "right"


  val `Map` = "Map"
  val `Tags` = "Tags"
  val `Tags.choosing` = "Tags.choosing"

  val `Activation.impossible` = "Activation.impossible"

  val `Upper.block` = "Upper.block"
  val `also.displayed.in.grid` = "also.displayed.in.grid"
  val `Description` = "Description"

  val `Height` = "Height"
  val `Width`  = "Width"

  val `Delete.block` = "Delete.block"
  val `Content` = "Content"
  val `Block` = "Block"
  val `Example.text` = "Example.text"
  val `Bg.color` = "Bg.color"
  val `Fg.color.of.sc.hint` = "Fg.color.of.sc.hint"
  val `Profile` = "Profile"

  val `File.is.not.a.picture` = "File.is.not.a.picture"
  val `Apply` = "Apply"
  val `Picture.editing` = "Picture.editing"
  val `Crop` = "Crop"

  val `Cannot.checksum.file` = "Cannot.checksum.file"

  val `Suggested.bg.colors` = "Suggested.bg.colors"

  val `Scale` = "Scale"

  /** Непереводится на разные языки, однако бывает нужно. */
  val `TODO` = "TODO"

  val `Preparing`                   = "Preparing"
  val `Uploading.file`              = "Uploading.file"

  val `Stretch.across`              = "Stretch.across"

  val `Main.block`                  = "Main.block"
  val `Main.blocks.are.used.to.display.ad.inside.the.grid` = "Main.blocks.are.used.to.display.ad.inside.the.grid"
  val `This.block.is.the.only.main` = "This.block.is.the.only.main"
  val `This.block.is.not.main`      = "This.block.is.not.main"
  val `Show`                        = "Show"
  val `Hide`                        = "Hide"
  val `No.main.blocks.First.block.is.main` = "No.main.blocks.First.block.is.main"
  val `Background` = "Background"
  val `Upload.file` = "Upload.file"

  val `No.tags.here` = "No.tags.here"
  val `No.tags.found.for.1.query` = "No.tags.found.for.1.query"

  // TODO "Вход" надо переименовать во что-то более очевидное.
  val `Login.page.title` = "Login.page.title"
  val `Login.to.sio` = "Login.to.sio"
  val `Go.to.node.ads` = "Go.to.node.ads"
  val `Personal.cabinet` = "Personal.cabinet"
  val `Suggest.io.Project` = "Suggest.io.Project"
  val `Suggest.io._transcription` = "Suggest.io._transcription"
  val `About.sio.node.id` = "About.sio.node.id"

  val `Rotation` = "Rotation"

  val `Ad.cards` = "Ad.cards"
  val `You.can.look.on.ads.in` = "You.can.look.on.ads.in"
  val `_ad.showcase.link` = "_ad.showcase.link"

  val `Create.ad` = "Create.ad"
  val `Show._ad` = "Show._ad"
  val `ad.Manage` = "ad.Manage"
  val `Show.ad.opened` = "Show.ad.opened"
  val `Always.outlined` = "Always.outlined"

  val `invalid_url` = "invalid_url"
  val `Welcome.screen` = "Welcome.screen"
  val `Welcome.bg.hint` = "Welcome.bg.hint"
  val `Node.photos` = "Node.photos"

  val `Bluetooth` = "Bluetooth"
  val `On` = "On"
  val `Off` = "Off"
  def onOff(isOn: Boolean): String =
    if (isOn) `On` else `Off`

  val `Clear` = "Clear"
  val `Search.start.typing` = "Search.start.typing"


  val `Cart` = "Cart"
  val `_order.Items` = "_order.Items"
  val `Delete.all` = "Delete.all"
  val `Node` = "Node"
  val `Your.cart.is.empty` = "Your.cart.is.empty"
  val `N.selected` = "N.selected"
  val `Total` = "Total"
  val `Reload` = "Reload"
  val `Go.to.payment.page` = "Go.to.payment.page"

  val `Bill.id` = "Bill.id"
  val `Bill.details` = "Bill.details"
  val `Sum` = "Sum"
  val `No.transactions.found` = "No.transactions.found"
  val `Payment.for.order.N` = "Payment.for.order.N"
  val `Transactions` = "Transactions"
  val `All` = "All"

  val `Nothing.found` = "Nothing.found"
  val `Nothing.to.moderate` = "Nothing.to.moderate"
  val `No.incoming.adv.requests` = "No.incoming.adv.requests"

  val `Lost.node` = "Lost.node"
  val `Approve` = "Approve"
  val `Refuse` = "Refuse"
  val `Reason` = "Reason"

  val `Next.node` = "Next.node"
  val `Previous.node` = "Previous.node"
  val `To.beginning` = "To.beginning"
  val `To.end` = "To.end"
  val `Moderation` = "Moderation"
  val `Moderation.needed` = "Moderation.needed"

  val `Moderate.all.nodes` = "Moderate.all.nodes"
  val `Moderate.requests.from.all.nodes` = "Moderate.requests.from.all.nodes"
  val `Go.to.moderation` = "Go.to.moderation"
  val `Nodes.management` = "Nodes.management"
  val `On.the.map` = "On.the.map"
  val `Does.not.adv.on.map` = "Does.not.adv.on.map"
  val `In.cart` = "In.cart"
  val `Adv.req.sent` = "Adv.req.sent"
  val `Will.be.adv` = "Will.be.adv"
  val `Adv.req.refused` = "Adv.req.refused"
  val `To.adv.on.map` = "To.adv.on.map"

  val `Layer`  = "Layer"
  val `Layers` = "Layers"
  val `Uppermost` = "Uppermost"
  val `Downmost` = "Downmost"
  val `Above` = "Above"
  val `Below` = "Below"
  val `Approved._adv` = "Approved._adv"
  val `Geolocation` = "Geolocation"

  val `Go.into` = "Go.into"
  val `Location.changed` = "Location.changed"
  val `Go.back` = "Go.back"
  val `Go.back.to.0` = "Go.back.to.0"

  val `Text.shadow` = "Text.shadow"
  val `Color` = "Color"
  val `Shadow.color` = "Shadow.color"

  val `No.ads.found.node.id` = "No.ads.found.node.id"

  val `GPS` = "GPS"
  val `0.uses.geoloc.to.find.ads` = "0.uses.geoloc.to.find.ads"
  val `0.uses.bt.to.find.ads.indoor` = "0.uses.bt.to.find.ads.indoor"
  val `Allow.0` = "Allow.0"
  val `Later` = "Later"
  val `You.can.enable.0.later.on.left.panel` = "You.can.enable.0.later.on.left.panel"
  val `Next` = "Next"
  val `Back` = "Back"
  val `Settings.done.0.ready.for.using` = "Settings.done.0.ready.for.using"
  val `_to.Finish` = "_to.Finish"

  val `GovServices.ESIA` = "GovServices.ESIA"
  val `ESIA._unabbrevated` = "ESIA._unabbrevated"

  /** Привести булёво значение к Yes или No.
    * И это потом можно в messages() передавать, для локализации ответа. */
  def yesNo(isYes: Boolean): String = {
    if (isYes) MsgCodes.`Yes`
    else MsgCodes.`No`
  }

  val `Password` = "Password"
  val `Login` = "Login"
  val `Username` = "Username"
  val `Not.my.pc` = "Not.my.pc"

  val `I.accept` = "I.accept"
  val `terms.of.service` = "terms.of.service"

  val `I.allow.personal.data.processing` = "I.allow.personal.data.processing"
  val `Your.email.addr` = "Your.email.addr"

  val `Input.text.from.picture` = "Input.text.from.picture"
  val `Sign.up` = "Sign.up"
  val `Password.recovery` = "Password.recovery"

  val `Email.example` = "Email.example"
  val `Phone.number.example` = "Phone.number.example"
  val `Phone.or.email` = "Phone.or.email"
  val `Mobile.phone.number` = "Mobile.phone.number"
  val `Code.from.sms` = "Code.from.sms"

  val `Registration.code` = "Registration.code"
  val `Sms.code.sent.to.your.phone.number.0` = "Sms.code.sent.to.your.phone.number.0"

  val `Type.password` = "Type.password"
  val `Retype.password` = "Retype.password"
  val `Type.current.password` = "Type.current.password"
  val `Type.new.password` = "Type.new.password"
  val `Confirm.new.password` = "Confirm.new.password"

  val `My.node` = "My.node"
  val `Type.previous.signup.data.to.start.password.recovery` = "Type.previous.signup.data.to.start.password.recovery"
  val `Forgot.password` = "Forgot.password"
  val `Password.change` = "Password.change"
  val `New.password.saved` = "New.password.saved"
  val `Invalid.password` = "Invalid.password"
  val `Inacceptable.password.format` = "Inacceptable.password.format"

  val `Changes.saved` = "Changes.saved"
  val `New.shop.created.fill.info` = "New.shop.created.fill.info"

  val `Expand` = "Expand"
  val `Dont.expand` = "Dont.expand"
  val `Line.height` = "Line.height"
  val `Try.again` = "Try.again"

  val `Application` = "Application"
  val `Download.application` = "Download.application"

  val `Predicate` = "Predicate"
  val `Node.ids.or.keys` = "Node.ids.or.keys"
  val `Flag.value` = "Flag.value"
  val `Ordering` = "Ordering"
  val `Not.indexed.text` = "Not.indexed.text"
  val `Type.text...` = "Type.text..."
  val `Size` = "Size"
  val `Original` = "Original"
  val `File` = "File"
  val `Storage` = "Storage"
  val `Metadata` = "Metadata"
  val `Type` = "Type"
  val `Picture` = "Picture"
  val `Mime.type` = "Mime.type"
  val `Media` = "Media"
  val `Open.file` = "Open.file"
  val `Go.to.node.0` = "Go.to.node.0"
  val `File.already.exist.on.node.0` = "File.already.exist.on.node.0"
  val `Replace.node.ids` = "Replace.node.ids"
  val `Append.to.node.ids` = "Append.to.node.ids"
  val `External.service` = "External.service"
  val `empty` = "empty"
  val `Info` = "Info"

  val `Download.0` = "Download.0"
  val `No.downloads.available` = "No.downloads.available"

  val `K._Kilo` = "K._Kilo"
  val `M._Mega` = "M._Mega"
  val `G._Giga` = "G._Giga"
  val `T._Tera` = "T._Tera"
  val `Number.frac.delim` = "Number.frac.delim"
  val `Number0.with.units1` = "Number0.with.units1"
  val `Prefixed0.metric.unit1` = "Prefixed0.metric.unit1"
  val `B._Bytes` = "B._Bytes"

  val `Choose...` = "Choose..."
  val `Operating.system.family` = "Operating.system.family"
  val `Malfunction` = "Malfunction"
  val `Open.0` = "Open.0"
  val `Install` = "Install"
  val `Settings` = "Settings"

  val `0.not.supported.on.this.platform.1` = "0.not.supported.on.this.platform.1"
  val `Install.app.for.access.to.0` = "Install.app.for.access.to.0"
  val `Browser` = "Browser"

  val `Notifications` = "Notifications"
  val `Notify.about.offers.nearby` = "Notify.about.offers.nearby"

  // TODO Переехать на mozilla fluent, чтобы отработать варианты "2 предложениЯ", "5 предложениЙ" и др.
  val `0.offers.nearby` = "0.offers.nearby"
  val `One.offer.nearby` = "One.offer.nearby"

  val `0._inDistance.1` = "0._inDistance.1"
  val `Show.offers.0` = "Show.offers.0"

  val `Title` = "Title"

  val `Background.mode` = "Background.mode"
  val `Background.monitoring.offers.nearby` = "Background.monitoring.offers.nearby"
  val `Daemon.toast.title` = "Daemon.toast.title"
  val `Daemon.toast.descr` = "Daemon.toast.descr"

  val `Unsupported.browser.or.fatal.failure` = "Unsupported.browser.or.fatal.failure"

  val `Not.connected` = "Not.connected"
  val `Retry` = "Retry"

  val `Showcase` = "Showcase"
  val `Recents` = "Recents"

  val `Screen` = "Screen"
  val `Unsafe.offset` = "Unsafe.offset"

  val `Distance` = "Distance"
  val `_to.Register._thing` = "_to.Register._thing"
  val `Add.beacon.to.account` = "Add.beacon.to.account"
  val `To.control.beacons.need.login` = "To.control.beacons.need.login"
  val `Parent.node` = "Parent.node"

  val `Logout.account` = "Logout.account"
  val `Logout` = "Logout"
  val `Are.you.sure.to.logout.account` = "Are.you.sure.to.logout.account"

  val `Mode` = "Mode"
  val `Ad.adv.manage` = "Ad.adv.manage"

  val `Bg.location` = "Bg.location"
  val `Bg.location.hint` = "Bg.location.hint"

  val `Debug` = "Debug"
  val `Version` = "Version"

  val `Outline` = "Outline"
  val `Transparent` = "Transparent"
  val `Define.manually` = "Define.manually"

  val `Events` = "Events"
  val `Action` = "Action"

  val `NFC` = "NFC"
  val `Read.radio.tags.on.tap` = "Read.radio.tags.on.tap"

  val `Radio.beacons` = "Radio.beacons"
  val `Node.type` = "Node.type"

  val `This.action.cannot.be.undone` = "This.action.cannot.be.undone"

  val `Write.nfc.tag` = "Write.nfc.tag"
  val `Make.nfc.tag.read.only` = "Make.nfc.tag.read.only"
  val `Bring.nfc.tag.to.device` = "Bring.nfc.tag.to.device"

  val `You.can.flash.nfc.tag.with.link.to.0` = "You.can.flash.nfc.tag.with.link.to.0"
  val `_nfc._to.current.ad` = "_nfc._to.current.ad"
  val `_nfc._to.current.node` = "_nfc._to.current.node"
  val `Also.you.can.make.nfc.tag.non.writable` = "Also.you.can.make.nfc.tag.non.writable"

  val `Language` = "Language"
  //val `Switch.language` = "Switch.lang"
  val `Select.your.lang` = "Select.your.lang"
  val `System._adjective` = "System._adjective"

  val `Requesting.permission` = "Requesting.permission"

}
