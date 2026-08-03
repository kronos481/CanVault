import type { PropsWithChildren } from 'react';
import type { AccessibilityProps, PressableProps, StyleProp, ViewStyle } from 'react-native';
import { Pressable } from 'react-native';
import Animated, {
  Easing,
  useAnimatedStyle,
  useReducedMotion,
  useSharedValue,
  withTiming,
} from 'react-native-reanimated';

import { motion } from '@/theme/tokens';

const ReanimatedPressable = Animated.createAnimatedComponent(Pressable);

type AnimatedPressableProps = PropsWithChildren<
  Omit<PressableProps, 'style' | 'onPressIn' | 'onPressOut'> &
    AccessibilityProps & {
      style?: StyleProp<ViewStyle>;
      pressedScale?: number;
    }
>;

export function AnimatedPressable({
  children,
  style,
  disabled,
  pressedScale = 0.985,
  ...props
}: AnimatedPressableProps) {
  const scale = useSharedValue(1);
  const reduceMotion = useReducedMotion();
  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.get() }],
  }));

  return (
    <ReanimatedPressable
      {...props}
      disabled={disabled}
      onPressIn={() => {
        if (!disabled && !reduceMotion) {
          scale.set(
            withTiming(pressedScale, {
              duration: motion.fast,
              easing: Easing.out(Easing.quad),
            }),
          );
        }
      }}
      onPressOut={() => {
        if (!reduceMotion) {
          scale.set(
            withTiming(1, {
              duration: motion.normal,
              easing: Easing.out(Easing.cubic),
            }),
          );
        }
      }}
      style={[style, animatedStyle]}
    >
      {children}
    </ReanimatedPressable>
  );
}
