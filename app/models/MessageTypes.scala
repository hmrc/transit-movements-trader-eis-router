package models

trait MessageType extends IeMetadata {
  def code: String
  def rootNode: String
}

sealed trait Directable extends MessageType
trait ArrivalMessage extends Directable
trait DepartureMessage extends Directable

object MessageType extends Enumerable.Implicits {

  case object ArrivalRejection          extends IeMetadata("IE008", "CC008A") with ArrivalMessage
  case object UnloadingPermission       extends IeMetadata("IE043", "CC043A") with ArrivalMessage
  case object UnloadingRemarksRejection extends IeMetadata("IE058", "CC058A") with ArrivalMessage
  case object GoodsReleased             extends IeMetadata("IE025", "CC025A") with ArrivalMessage

  case object PositiveAcknowledgement        extends IeMetadata("IE928", "CC928A") with DepartureMessage
  case object MrnAllocated                   extends IeMetadata("IE028", "CC028A") with DepartureMessage
  case object DeclarationRejected            extends IeMetadata("IE016", "CC016A") with DepartureMessage
  case object ControlDecisionNotification    extends IeMetadata("IE060", "CC060A") with DepartureMessage
  case object NoReleaseForTransit            extends IeMetadata("IE051", "CC051B") with DepartureMessage
  case object ReleaseForTransit              extends IeMetadata("IE029", "CC029B") with DepartureMessage
  case object CancellationDecision           extends IeMetadata("IE009", "CC009A") with DepartureMessage
  case object WriteOffNotification           extends IeMetadata("IE045", "CC045A") with DepartureMessage
  case object GuaranteeNotValid              extends IeMetadata("IE055", "CC055A") with DepartureMessage

  val departureValues: Seq[Directable] = Seq(PositiveAcknowledgement, MrnAllocated, DeclarationRejected, ControlDecisionNotification, NoReleaseForTransit,
    ReleaseForTransit, CancellationDecision, WriteOffNotification, GuaranteeNotValid)

  val arrivalValues: Seq[Directable] = Seq(ArrivalRejection, UnloadingPermission, UnloadingRemarksRejection, GoodsReleased)

  val validMessages: Seq[Directable] = departureValues ++ arrivalValues

  val allMessages: Seq[MessageType] = departureValues ++ arrivalValues

  implicit val enumerable: Enumerable[MessageType] =
    Enumerable(allMessages.map(v => v.code -> v): _*)
}
