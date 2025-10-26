import { Canvas, Circle, Group, interpolateColors, Path, Skia } from '@shopify/react-native-skia';
import * as Haptics from 'expo-haptics';
import React, { useCallback, useMemo } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import {
    Easing,
    interpolate,
    useDerivedValue,
    useSharedValue,
    withTiming
} from 'react-native-reanimated';
import { COLOR_GRAY, COLOR_RED, COLORS, PARTICLE_COLORS } from './constants';

const HEART_SVG =
  'M20.884 13.19c-1.351 2.48-4.001 5.12-8.379 7.67l-.503.3-.504-.3c-4.379-2.55-7.029-5.19-8.382-7.67-1.36-2.5-1.41-4.86-.514-6.67.887-1.79 2.647-2.91 4.601-3.01 1.651-.09 3.368.56 4.798 2.01 1.429-1.45 3.146-2.1 4.796-2.01 1.954.1 3.714 1.22 4.601 3.01.896 1.81.846 4.17-.514 6.67z';

const CANVAS_SIZE = 58;
const HEART_SIZE = 26;
const HEART_CENTER = CANVAS_SIZE / 2;

const D = 1000;
const easeOutC = Easing.bezier(0.21, 0.61, 0.35, 1);

const N_GROUPS = 7;
const N_PARTICLES = 2;
const GROUP_DISTR_R = 20;
const PARTICLE_D = 2;
const PARTICLE_OFF_ANGLE = (40 * Math.PI) / 180;

interface LikeButtonSkiaProps {
  initialLiked?: boolean;
  onToggle?: (liked: boolean) => void;
  debugMode?: boolean;
}

export function XHeartAnimation({
  initialLiked = false,
  onToggle,
  debugMode = false
}: LikeButtonSkiaProps) {
  const [isLiked, setIsLiked] = React.useState(initialLiked);
  const [showParticles, setShowParticles] = React.useState(false);

  const progress = useSharedValue(initialLiked ? 1 : 0);

  const isLiking = useSharedValue(initialLiked);

  const particles = useMemo(() => {
    const particleList: Array<{
      x: number;
      y: number;
      startColor: string;
      endColor: string;
      pairIndex: number;
    }> = [];
    const groupBaseAngle = (2 * Math.PI) / N_GROUPS;
    const particleBaseAngle = (2 * Math.PI) / N_PARTICLES;

    for (let i = 0; i < N_GROUPS; i++) {
      const groupCurrAngle = i * groupBaseAngle - Math.PI / 2;

      const xg = GROUP_DISTR_R * Math.cos(groupCurrAngle);
      const yg = GROUP_DISTR_R * Math.sin(groupCurrAngle);

      const colorSet = PARTICLE_COLORS[i];

      for (let j = 0; j < N_PARTICLES; j++) {
        const particleCurrAngle = groupCurrAngle + PARTICLE_OFF_ANGLE + j * particleBaseAngle;

        const xs = xg + PARTICLE_D * Math.cos(particleCurrAngle);
        const ys = yg + PARTICLE_D * Math.sin(particleCurrAngle);

        const gradient = j === 0 ? colorSet.first : colorSet.second;

        particleList.push({
          x: xs,
          y: ys,
          startColor: gradient.start,
          endColor: gradient.end,
          pairIndex: j,
        });
      }
    }

    return particleList;
  }, []);

  const handlePress = useCallback(() => {
    const newLiked = !isLiked;
    setIsLiked(newLiked);

    if (newLiked) {
      setShowParticles(true);

      isLiking.value = true;

      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);

      progress.value = withTiming(1, {
        duration: D,
        easing: easeOutC,
      });
    } else {
      setShowParticles(false);

      isLiking.value = false;

      progress.value = withTiming(0, {
        duration: 200,
        easing: Easing.ease
      });
    }

    if (onToggle) {
      try {
        onToggle(newLiked);
      } catch (error) {
        console.error('Error in onToggle callback:', error);
      }
    }
  }, [isLiked, progress, isLiking, onToggle]);

  const heartTransform = useDerivedValue(() => {
    if (progress.value === 0) return [{ scale: 1 }];
    const scale = interpolate(
      progress.value,
      [0, 0.75, 0.9, 1],
      [0, 1.15, 1, 1.15, 1]
    );

    return [{ scale }];
  });

  const ringTransform = useDerivedValue(() => {
    if (!isLiking.value) {
      return [{ scale: 0 }];
    }

    const scale = interpolate(
      progress.value,
      [0, 0.3, 1],
      [0, 1, 1]
    );
    return [{ scale }];
  });

  const circleOuterRadius = useDerivedValue(() => {
    return 16;
  });

  const circleInnerRadius = useDerivedValue(() => {
    return interpolate(
      progress.value,
      [0, 0.3, 0.5, 0.7, 1],
      [0, 0, 5, 12, 16]
    );
  });

  const ringPath = useDerivedValue(() => {
    const outerR = circleOuterRadius.value;
    const innerR = circleInnerRadius.value;

    const path = Skia.Path.Make();
    path.setFillType(1);

    path.addCircle(HEART_CENTER, HEART_CENTER, outerR);

    if (innerR > 0) {
      path.addCircle(HEART_CENTER, HEART_CENTER, innerR);
    }

    return path;
  });

  const circleOpacity = useDerivedValue(() => {
    return interpolate(
      progress.value,
      [0, 0.1, 0.3, 0.5, 0.8, 1],
      [0, 0.8, 0.9, 0.7, 0.3, 0]
    );
  });

  const circleColor = useDerivedValue(() => {
    return interpolateColors(
      progress.value,
      [0, 0.2, 0.4, 0.6, 1],
      [COLORS.pink, COLORS.pink, COLORS.purple, COLORS.lavender, COLORS.lavender]
    );
  });

  const heartColor = useDerivedValue(() => {
    return interpolateColors(
      progress.value,
      [0, 0.03, 0.20, 0.30, 1],
      [COLOR_GRAY, COLOR_GRAY, COLOR_RED, COLOR_RED, COLOR_RED]
    );
  });

  const heartStyle = useDerivedValue(() => {
    return progress.value === 0 || progress.value < 0.03 ? 'stroke' : 'fill';
  });

  const heartStrokeWidth = useDerivedValue(() => {
    return progress.value === 0 || progress.value < 0.03 ? 2 : 0;
  });

  const STAGGER_DELAY = 0.08;

  const createParticleAnimation = (pairIndex: number) => {
    const timeOffset = pairIndex * STAGGER_DELAY;

    return {
      radius: useDerivedValue(() => {
        const adjustedProgress = progress.value - timeOffset;

        if (adjustedProgress < 0.20) {
          return 0;
        } else if (adjustedProgress < 0.25) {
          return interpolate(adjustedProgress, [0.20, 0.25], [0, GROUP_DISTR_R]);
        }
        return interpolate(
          adjustedProgress,
          [0.25, 1.0],
          [GROUP_DISTR_R, GROUP_DISTR_R * 1.25]
        );
      }),

      size: useDerivedValue(() => {
        const adjustedProgress = progress.value - timeOffset;

        if (adjustedProgress < 0.20) {
          return 0;
        } else if (adjustedProgress < 0.25) {
          return interpolate(adjustedProgress, [0.20, 0.25], [0, 1.2]);
        }
        return interpolate(
          adjustedProgress,
          [0.25, 0.60, 0.85, 1.0],
          [1.2, 1.4, 0.8, 0]
        );
      }),

      opacity: useDerivedValue(() => {
        const adjustedProgress = progress.value - timeOffset;

        if (adjustedProgress < 0.20) {
          return 0;
        } else if (adjustedProgress < 0.25) {
          return interpolate(adjustedProgress, [0.20, 0.25], [0, 1]);
        } else if (adjustedProgress < 0.60) {
          return 1;
        }
        return interpolate(adjustedProgress, [0.60, 1.0], [1, 0]);
      })
    };
  };

  const heartPath = Skia.Path.MakeFromSVGString(HEART_SVG);
  if (!heartPath) return null;

  const heartMatrix = Skia.Matrix();
  heartMatrix.translate(HEART_CENTER, HEART_CENTER);
  heartMatrix.scale(HEART_SIZE / 24, HEART_SIZE / 24);
  heartMatrix.translate(-12, -12);

  return (
    <View style={styles.container}>
      <Pressable onPress={debugMode ? undefined : handlePress} style={styles.button}>
        <Canvas style={styles.canvas} pointerEvents="none">
          <Group
            transform={ringTransform}
            origin={{ x: HEART_CENTER, y: HEART_CENTER }}
          >
            <Path
              path={ringPath}
              color={circleColor}
              opacity={circleOpacity}
              style="fill"
            />
          </Group>

          {particles.map((particle, index) => {
            const animation = createParticleAnimation(particle.pairIndex);

            const particleX = useDerivedValue(() => {
              const scale = animation.radius.value / GROUP_DISTR_R;
              return HEART_CENTER + particle.x * scale;
            });

            const particleY = useDerivedValue(() => {
              const scale = animation.radius.value / GROUP_DISTR_R;
              return HEART_CENTER + particle.y * scale;
            });

            const finalOpacity = useDerivedValue(() => {
              return showParticles ? animation.opacity.value : 0;
            });

            const particleColor = useDerivedValue(() => {
              return interpolateColors(
                progress.value,
                [0.20, 1.0],
                [particle.startColor, particle.endColor]
              );
            });

            return (
              <Circle
                key={`particle-${index}`}
                cx={particleX}
                cy={particleY}
                r={animation.size}
                color={particleColor}
                opacity={finalOpacity}
                style="fill"
              />
            );
          })}

          <Group
            transform={heartTransform}
            origin={{ x: HEART_CENTER, y: HEART_CENTER }}
          >
            <Path
              path={heartPath}
              color={heartColor}
              style={heartStyle}
              strokeWidth={heartStrokeWidth}
              matrix={heartMatrix}
            />
          </Group>
        </Canvas>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  button: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  canvas: {
    width: CANVAS_SIZE,
    height: CANVAS_SIZE,
    // backgroundColor: "#EA69C830",
    // borderRadius: 100
  },
  count: {
    fontSize: 13,
    color: COLOR_GRAY,
    fontWeight: '400',
    marginTop: -8,
  },
  countLiked: {
    color: COLOR_RED,
  },
});
