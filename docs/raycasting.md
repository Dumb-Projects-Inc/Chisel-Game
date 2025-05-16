# Raycasting

## What is Raycasting?

Raycasting is a technique used to trace a ray (a straight line) from a starting point in a given direction and detect what it hits in a 2D grid-based map. It is commonly used in 2.5D rendering engines (like _Wolfenstein 3D_) to simulate visibility, detect walls, or calculate distances. DDA is used to decide smart stepping values.

---

## How It Works (in this implementation)

This raycaster casts a ray from a given `(x, y)` position in a direction defined by an angle `θ`. The map is a grid of square tiles, and the goal is to determine how far the ray travels before it hits a wall.

The ray progresses in steps, always moving to the next horizontal or vertical gridline it would intersect. In implementation, this results in two rays being cast. One, which hits every horizontal gridline, and one which hits every vertical. When stepping, only the shortest ray is stepped to it's next intersection. The shortest ray is considered the current position.

---

## Math Used

### 1. Initial Setup

From the start point `(x₀, y₀)` and angle `θ`, the ray calculates:

- The first vertical and horizontal intersections from the start point:
  - Horizontal:
    ```
    y = next integer y line (ceil or floor based on direction)
    x = x₀ + (y - y₀) * cot(θ)
    ```
  - Vertical:
    ```
    x = next integer x line
    y = y₀ + (x - x₀) * tan(θ)
    ```

### 2. Step Increments (DDA)

Each subsequent step adds a fixed delta to the horizontal or vertical ray:

- Horizontal delta:

```
(Δx, Δy) = (±cot(θ), ±1)

```

- Vertical delta:

```
(Δx, Δy) = (±1, ±tan(θ))

```

Signs depend on whether the ray is pointing north/south or east/west.

### 3. Distance Comparison

At each step, the distance that each ray is calculated. Observe, that the distance isn't squared, as it isn't necessary to compare two distances:

```
distH = (hx - x₀)² + (hy - y₀)²
distV = (vx - x₀)² + (vy - y₀)²

```

The smaller one determines the next position to step.

### 4. Termination

The ray continues stepping until:

- It hits a wall (`stop == true`), or
- It reaches a maximum number of steps.

The final output is the hit position and the squared distance from the start.
