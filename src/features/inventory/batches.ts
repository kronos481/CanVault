export function splitBatchQuantity(
  totalQuantity: number,
  movedQuantity: number,
): { remaining: number; moved: number } {
  if (
    !Number.isInteger(totalQuantity) ||
    !Number.isInteger(movedQuantity) ||
    totalQuantity < 1 ||
    movedQuantity < 1 ||
    movedQuantity > totalQuantity
  ) {
    throw new Error(
      'Batch quantities must be positive integers and moved quantity cannot exceed total.',
    );
  }
  return { remaining: totalQuantity - movedQuantity, moved: movedQuantity };
}
