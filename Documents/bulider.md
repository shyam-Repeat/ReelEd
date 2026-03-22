import { useState } from 'react';
import { useDrag, useDrop } from 'react-dnd';
import type { DragSourceMonitor } from 'react-dnd';

interface NumberTile {
  id: string;
  number: number;
  position: { x: number; y: number };
}

interface DraggableNumberProps {
  tile: NumberTile;
  isMatched: boolean;
}

const DraggableNumber = ({ tile, isMatched }: DraggableNumberProps) => {
  const [{ isDragging }, drag] = useDrag({
    type: 'number',
    item: tile,
    canDrag: !isMatched,
    collect: (monitor: DragSourceMonitor) => ({
      isDragging: monitor.isDragging(),
    }),
  });

  if (isMatched) return null;

  return (
    <div
      ref={drag}
      className="absolute text-blue-600 text-5xl font-bold cursor-move select-none hover:scale-110 transition-transform"
      style={{
        left: `${tile.position.x}%`,
        top: `${tile.position.y}%`,
        opacity: isDragging ? 0 : 1,
      }}
    >
      {tile.number}
    </div>
  );
};

interface DropTargetProps {
  targetNumber: number;
  isMatched: boolean;
  onDrop: (item: NumberTile) => void;
}

const DropTarget = ({ targetNumber, isMatched, onDrop }: DropTargetProps) => {
  const [{ isOver, canDrop }, drop] = useDrop({
    accept: 'number',
    drop: (item: NumberTile) => {
      if (item.number === targetNumber) {
        onDrop(item);
      }
    },
    canDrop: (item: NumberTile) => item.number === targetNumber && !isMatched,
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      canDrop: monitor.canDrop(),
    }),
  });

  const textColor = isMatched
    ? 'text-green-600'
    : isOver && canDrop
    ? 'text-blue-400'
    : 'text-gray-400';

  return (
    <div
      ref={drop}
      className={`text-6xl font-bold ${textColor} transition-colors`}
    >
      {targetNumber}
    </div>
  );
};

export function DragDropQuiz() {
  const [currentTarget] = useState(1);
  const [matchedNumbers, setMatchedNumbers] = useState<number[]>([]);
  
  // Generate random positions for numbers 1-9 scattered around the screen
  const [tiles] = useState<NumberTile[]>(() => {
    const positions = [
      { x: 10, y: 15 },
      { x: 75, y: 10 },
      { x: 5, y: 45 },
      { x: 80, y: 40 },
      { x: 15, y: 75 },
      { x: 70, y: 70 },
      { x: 40, y: 10 },
      { x: 85, y: 75 },
      { x: 50, y: 75 },
    ];
    
    return Array.from({ length: 9 }, (_, i) => ({
      id: `tile-${i + 1}`,
      number: i + 1,
      position: positions[i],
    }));
  });

  const handleDrop = (item: NumberTile) => {
    setMatchedNumbers([...matchedNumbers, item.number]);
  };

  const isMatched = matchedNumbers.includes(currentTarget);

  return (
    <div className="relative w-full h-full bg-gradient-to-br from-purple-100 to-blue-100 overflow-hidden">
      {/* Center drop target */}
      <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 flex flex-col items-center gap-4">
        <DropTarget
          targetNumber={currentTarget}
          isMatched={isMatched}
          onDrop={handleDrop}
        />
        {isMatched && (
          <p className="text-xl font-semibold text-green-600 animate-bounce">
            ✓ Correct Match!
          </p>
        )}
      </div>

      {/* Scattered draggable numbers */}
      {tiles.map((tile) => (
        <DraggableNumber
          key={tile.id}
          tile={tile}
          isMatched={matchedNumbers.includes(tile.number)}
        />
      ))}

      {/* Instructions */}
      <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 bg-white/90 px-6 py-3 rounded-lg shadow-md">
        <p className="text-gray-700 font-medium">
          Drag the number <span className="font-bold text-blue-600">{currentTarget}</span> to the center to match
        </p>
      </div>
    </div>
  );
}