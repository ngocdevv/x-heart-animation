import { BottomSheetModalProvider } from '@gorhom/bottom-sheet';
import { NavigationContainer } from '@react-navigation/native';
import React from 'react';
import { StyleSheet, View } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import 'react-native-reanimated';
import { XHeartAnimation } from './src/components/x-heart-animation';

export default function App() {
  const [liked, setLiked] = React.useState(false);
  return (
    <GestureHandlerRootView style={styles.fill}>
      <BottomSheetModalProvider>
        <NavigationContainer>
          <View style={styles.fill}>
            <XHeartAnimation
              initialLiked={false}
              onToggle={(liked) => {
                console.log('Liked:', liked);
              }}
            />
          </View>
        </NavigationContainer>
      </BottomSheetModalProvider>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({
  fill: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor:"black"
  },
});

