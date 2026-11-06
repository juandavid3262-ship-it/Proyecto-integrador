import { Suspense } from 'react'
import { Canvas } from '@react-three/fiber'
import { GranizadoScene } from './components/GranizadoScene'

/**
 * Página lista para Lovable.
 * Dependencias: three, @react-three/fiber, @react-three/drei.
 * El logo vive en src/assets/freskos-logo.png.
 */
export default function App() {
  return (
    <main className="stage">
      <Canvas
        shadows
        dpr={[1, 1.75]}
        camera={{ position: [1.25, 2.05, 6.35], fov: 32, near: 0.1, far: 50 }}
        gl={{ antialias: true, alpha: false }}
      >
        <color attach="background" args={['#000000']} />
        <Suspense fallback={null}>
          <GranizadoScene />
        </Suspense>
      </Canvas>
      <div className="vignette" />
      <div className="overlay">
        <h1 className="brand">FRESKOS</h1>
        <p className="tag">GRANIZADO · GIRA EN SU EJE</p>
      </div>
    </main>
  )
}
