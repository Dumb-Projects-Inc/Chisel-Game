from manimlib import *
import numpy as np
import math


class CodeToMap(Scene):
    def cast_ray(
        self, start_pos, direction, grid, cell_size, max_dist=5.0, step_size=0.05
    ):
        """
        Cast a ray from start_pos in direction. Stop at first wall.
        Returns the end point of the ray.
        """
        pos = np.array(start_pos[:2])  # 2D
        for _ in range(int(max_dist / step_size)):
            pos += direction[:2] * step_size
            j = int((pos[0] / cell_size) + len(grid[0]) / 2)
            i = int(len(grid) / 2 - (pos[1] / cell_size))
            if 0 <= i < len(grid) and 0 <= j < len(grid[0]):
                if grid[i][j] == 1:
                    break
        return np.array([pos[0], pos[1], 0.0])

    def construct(self):
        data = [
            [1, 1, 1, 1, 1, 1, 1, 1],
            [1, 0, 0, 0, 0, 1, 0, 1],
            [1, 0, 1, 0, 0, 0, 0, 1],
            [1, 0, 1, 0, 0, 0, 0, 1],
            [1, 1, 1, 1, 1, 1, 1, 1],
        ]

        cell_size = 0.8
        mobjects = []

        rows = len(data)
        cols = len(data[0])

        map_label = Text("map =", font_size=32)
        label_x = (-cols / 2 - 1.5) * cell_size
        label_y = (rows / 2 - 0.5) * cell_size
        map_label.move_to(label_x * RIGHT + label_y * UP)
        self.play(FadeIn(map_label))

        # Create the number grid
        for i, row in enumerate(data):
            for j, val in enumerate(row):
                text = Text(str(val), font_size=28)
                text.move_to(
                    (j - cols / 2 + 0.5) * cell_size * RIGHT
                    + (rows / 2 - i - 0.5) * cell_size * UP
                )
                mobjects.append(text)

        number_grid = VGroup(*mobjects)
        self.play(FadeIn(number_grid))
        self.wait()

        transforms = []
        for i, val_text in enumerate(mobjects):
            val = int(val_text.text)
            square = Square(side_length=cell_size)
            square.move_to(val_text.get_center())
            if val == 1:
                square.set_fill(GREY, opacity=1)
            else:
                square.set_fill(WHITE, opacity=1)
            square.set_stroke(BLACK, width=1)
            transforms.append(Transform(val_text, square))

        self.play(*transforms, run_time=2)
        self.wait()

        # Add the player
        player_grid_pos = (2, 3)  # (row, col)
        player_x = (player_grid_pos[1] - cols / 2 + 0.5) * cell_size
        player_y = (rows / 2 - player_grid_pos[0] - 0.5) * cell_size
        player_pos = np.array([player_x, player_y, 0])

        player = Dot(point=player_pos, radius=0.08)
        player.set_fill(GREEN)

        self.play(FadeIn(player))

        # Add FOV lines
        fov_angle = math.radians(75)

        base_angle = math.radians(135)  # Aim
        angle_left = base_angle - fov_angle / 2
        angle_right = base_angle + fov_angle / 2

        dir_left = np.array([math.cos(angle_left), math.sin(angle_left), 0])
        dir_right = np.array([math.cos(angle_right), math.sin(angle_right), 0])

        end_left = self.cast_ray(player_pos, dir_left, data, cell_size)
        end_right = self.cast_ray(player_pos, dir_right, data, cell_size)

        line_left = Line(player_pos, end_left, color=YELLOW)
        line_right = Line(player_pos, end_right, color=YELLOW)

        self.play(GrowArrow(line_left), GrowArrow(line_right))
        self.wait()

        # rays
        num_rays = 5
        ray_angles = np.linspace(angle_left, angle_right, num_rays)
        ray_distances = []

        ray_lines = VGroup()
        ray_labels = VGroup()

        for i, angle in enumerate(ray_angles):
            direction = np.array([math.cos(angle), math.sin(angle), 0])
            end_point = self.cast_ray(player_pos, direction, data, cell_size)

            # Ray line
            ray = Line(player_pos, end_point, color=RED, stroke_width=2)
            ray_lines.add(ray)

            # Distance label
            dist = np.linalg.norm(end_point - player_pos)
            ray_distances.append(dist)
            label_text = f"ray_{i} = {dist:.4f}"
            label = Text(label_text, font="Courier", font_size=24)

            # Stack vertically
            x_offset = 0
            y_offset = -((rows / 2) + 1.0 + i * 0.5) * cell_size  # Stack down
            label.move_to(np.array([x_offset, y_offset + 0.5, 0]))

            ray_labels.add(label)

        self.play(*[GrowFromPoint(ray, player_pos) for ray in ray_lines])
        self.play(FadeIn(ray_labels))
        self.wait()

        everything_except_labels = VGroup(
            number_grid, map_label, player, line_left, line_right, ray_lines
        )
        self.play(FadeOut(everything_except_labels))
        ray_labels.generate_target()
        ray_labels.target.to_corner(UL).shift(DOWN * 0.5 + RIGHT * 0.5)

        self.play(MoveToTarget(ray_labels), run_time=1.5)
        self.wait()

        # Wall height formulas
        c = 3.5
        wall_labels = VGroup()
        for i, dist in enumerate(ray_distances):
            if dist <= 0 or not np.isfinite(dist):
                dist = 0.01  # avoid divide by zero

            formula = Text(
                f"h_{i} = {c:.1f} / {dist:.2f}", font="Courier", font_size=24
            )
            formula.next_to(ray_labels[i], RIGHT, buff=0.5)
            wall_labels.add(formula)

        self.play(FadeIn(wall_labels))
        self.wait()

        # Morph into numeric value
        wall_numeric_labels = VGroup()
        for i, dist in enumerate(ray_distances):
            h = c / dist
            numeric = Text(f"h_{i} = {h:.2f}", font="Courier", font_size=24)
            numeric.move_to(wall_labels[i].get_center())
            wall_numeric_labels.add(numeric)

        self.play(
            *[
                Transform(wall_labels[i], wall_numeric_labels[i])
                for i in range(len(wall_labels))
            ]
        )
        self.wait()

        bar_group = VGroup()
        screen_start = -((num_rays - 1) / 2) * 0.35  # center bars

        for i, raw_dist in enumerate(ray_distances):
            ray_angle = ray_angles[i]
            view_angle = base_angle
            angle_offset = ray_angle - view_angle

            corrected_dist = raw_dist * math.cos(angle_offset)
            corrected_dist = max(corrected_dist, 0.01)
            h = c / corrected_dist

            bar = Rectangle(height=h, width=0.3)
            bar.set_fill(WHITE, opacity=1)
            bar.set_stroke(BLACK, width=1)

            x = screen_start + (num_rays - 1 - i) * 0.35
            bar.move_to(np.array([x, 0, 0]))
            bar_group.add(bar)

        self.play(FadeIn(bar_group))
        self.wait()

        self.play(
            FadeOut(ray_labels),
            FadeOut(wall_numeric_labels),
            FadeOut(wall_labels),
            FadeOut(bar_group),
        )
        self.wait()

        num_rays = 120
        ray_angles = np.linspace(angle_left, angle_right, num_rays)

        bar_group = VGroup()
        animations = []
        screen_start = -((num_rays - 1) / 2) * 0.12

        for i, angle in enumerate(ray_angles):
            direction = np.array([math.cos(angle), math.sin(angle), 0])
            end_point = self.cast_ray(player_pos, direction, data, cell_size)

            raw_dist = np.linalg.norm(end_point - player_pos)
            angle_offset = angle - base_angle
            corrected_dist = max(raw_dist * math.cos(angle_offset), 0.01)
            h = c / corrected_dist

            bar = Rectangle(height=0.01, width=0.12)
            bar.set_fill(WHITE, opacity=1)
            bar.set_stroke(BLACK, width=0.5)

            x = screen_start + (num_rays - 1 - i) * 0.12
            bar.move_to(np.array([x, 0, 0]))

            # Animate stretch
            scale_factor = h / 0.01
            animations.append(bar.animate.stretch(scale_factor, dim=1))

            bar_group.add(bar)

        self.play(*animations, run_time=2)
        self.wait()

        self.wait()
