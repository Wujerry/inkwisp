export interface PredictionSwipePoint {
  x: number;
  y: number;
}

export type PredictionSwipeResult = "accept" | "ignore";
export type PredictionDragIntent = "pending" | "horizontal" | "vertical";

const MIN_HORIZONTAL_DISTANCE = 72;
const MAX_VERTICAL_DISTANCE = 48;
const INTENT_DISTANCE = 12;
const HORIZONTAL_DOMINANCE = 1.5;
const VERTICAL_DOMINANCE = 1.15;

export function classifyPredictionDragIntent(
  start: PredictionSwipePoint,
  current: PredictionSwipePoint,
): PredictionDragIntent {
  const deltaX = Math.abs(current.x - start.x);
  const deltaY = Math.abs(current.y - start.y);
  if (Math.max(deltaX, deltaY) < INTENT_DISTANCE) return "pending";
  if (deltaX >= INTENT_DISTANCE && deltaX >= deltaY * HORIZONTAL_DOMINANCE) return "horizontal";
  if (deltaY >= INTENT_DISTANCE && deltaY >= deltaX * VERTICAL_DOMINANCE) return "vertical";
  return "pending";
}

export function classifyPredictionSwipe(
  start: PredictionSwipePoint,
  end: PredictionSwipePoint,
): PredictionSwipeResult {
  const deltaX = end.x - start.x;
  const deltaY = Math.abs(end.y - start.y);
  return deltaX > MIN_HORIZONTAL_DISTANCE && deltaY < MAX_VERTICAL_DISTANCE
    ? "accept"
    : "ignore";
}
