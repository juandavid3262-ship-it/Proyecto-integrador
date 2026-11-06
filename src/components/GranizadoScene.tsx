import { useLayoutEffect, useMemo, useRef } from 'react'
import { useFrame } from '@react-three/fiber'
import {
  ContactShadows,
  Environment,
  Lightformer,
  MeshTransmissionMaterial,
  OrbitControls,
  useTexture,
} from '@react-three/drei'
import * as THREE from 'three'
import logoUrl from '../assets/freskos-logo.png'

/**
 * Granizado 3D de FRESKOS.
 * El vaso, el hielo y el logo giran juntos sobre el eje vertical del vaso.
 *
 * En Lovable:
 *  1. npm install three @react-three/fiber @react-three/drei
 *  2. Copia este archivo a src/components/GranizadoScene.tsx
 *  3. Copia el logo a src/assets/freskos-logo.png
 *  4. Monta <GranizadoScene /> dentro de un <Canvas> con fondo negro.
 */

const LOGO_ASPECT = 960 / 520

const CUP: ReadonlyArray<readonly [number, number]> = [
  [0.0, 0.0],
  [0.4, 0.0],
  [0.43, 0.05],
  [0.46, 0.18],
  [0.5, 0.55],
  [0.56, 1.0],
  [0.63, 1.5],
  [0.72, 2.0],
  [0.8, 2.28],
  [0.845, 2.42],
]

const SLUSH: ReadonlyArray<readonly [number, number]> = [
  [0.0, 0.1],
  [0.3, 0.1],
  [0.34, 0.2],
  [0.4, 0.55],
  [0.46, 1.0],
  [0.52, 1.5],
  [0.6, 2.0],
  [0.66, 2.22],
  [0.73, 2.36],
  [0.7, 2.5],
  [0.52, 2.72],
  [0.28, 2.92],
  [0.08, 3.05],
  [0.0, 3.1],
]

const NOISE = /* glsl */ `
float hash(vec3 p) {
  p = fract(p * 0.3183099 + vec3(0.11, 0.17, 0.13));
  p *= 17.0;
  return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}
float noise(vec3 x) {
  vec3 i = floor(x);
  vec3 f = fract(x);
  f = f * f * (3.0 - 2.0 * f);
  return mix(
    mix(mix(hash(i), hash(i + vec3(1.0, 0.0, 0.0)), f.x),
        mix(hash(i + vec3(0.0, 1.0, 0.0)), hash(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
    mix(mix(hash(i + vec3(0.0, 0.0, 1.0)), hash(i + vec3(1.0, 0.0, 1.0)), f.x),
        mix(hash(i + vec3(0.0, 1.0, 1.0)), hash(i + vec3(1.0, 1.0, 1.0)), f.x), f.y),
    f.z
  );
}
`

const slushVertex = /* glsl */ `
uniform float uTime;
varying vec3 vWorld;
varying vec3 vObj;
${NOISE}
void main() {
  vec3 p = position;
  float n = noise(p * 4.8 + vec3(0.0, uTime * 0.04, 0.0));
  float dome = smoothstep(2.05, 3.0, p.y);
  p += normal * (n - 0.5) * dome * 0.04;
  vec4 world = modelMatrix * vec4(p, 1.0);
  vWorld = world.xyz;
  vObj = p;
  gl_Position = projectionMatrix * viewMatrix * world;
}
`

const slushFragment = /* glsl */ `
precision highp float;
uniform float uTime;
varying vec3 vWorld;
varying vec3 vObj;
${NOISE}
void main() {
  vec3 N = normalize(cross(dFdy(vWorld), dFdx(vWorld)));
  vec3 V = normalize(cameraPosition - vWorld);
  vec3 L = normalize(vec3(0.42, 0.86, 0.32));
  float ndl = clamp(dot(N, L), 0.0, 1.0);
  float fres = pow(1.0 - clamp(dot(N, V), 0.0, 1.0), 2.6);

  float grain = noise(vObj * 5.5);
  float flake = noise(vObj * 26.0 + vec3(uTime * 0.03, 0.2, 0.0));
  float micro = noise(vObj * 54.0);

  vec3 deep = vec3(0.62, 0.0, 0.1);
  vec3 berry = vec3(0.95, 0.06, 0.24);
  vec3 pink = vec3(1.0, 0.32, 0.48);
  vec3 frost = vec3(1.0, 0.9, 0.93);

  float h = smoothstep(0.12, 2.85, vObj.y);
  vec3 col = mix(deep, berry, smoothstep(0.0, 0.5, h));
  col = mix(col, pink, smoothstep(0.42, 0.9, h));
  col = mix(col, vec3(0.72, 0.08, 0.18), smoothstep(0.62, 0.95, grain) * 0.22);
  col = mix(col, frost, smoothstep(0.74, 0.96, flake) * 0.5);
  col = mix(col, vec3(1.0), smoothstep(0.88, 0.99, micro) * 0.28);
  col = mix(col, vec3(1.0, 0.75, 0.82), smoothstep(2.5, 3.05, vObj.y) * 0.4);

  col *= 0.78 + 0.4 * ndl;
  col += frost * fres * 0.28;
  float spec = pow(clamp(dot(reflect(-L, N), V), 0.0, 1.0), 48.0);
  col += spec * 0.38;
  col = mix(col, vec3(0.5, 0.0, 0.1), smoothstep(0.7, 0.12, vObj.y) * 0.28);

  gl_FragColor = vec4(col, 1.0);
}
`

function radiusAt(profile: ReadonlyArray<readonly [number, number]>, y: number) {
  if (y <= profile[0][1]) return profile[0][0]
  for (let i = 0; i < profile.length - 1; i += 1) {
    const [r0, y0] = profile[i]
    const [r1, y1] = profile[i + 1]
    if (y <= y1) {
      const span = y1 - y0 || 1
      const t = (y - y0) / span
      return r0 + (r1 - r0) * t
    }
  }
  return profile[profile.length - 1][0]
}

function lathe(profile: ReadonlyArray<readonly [number, number]>, segments: number) {
  const points = profile.map(([radius, y]) => new THREE.Vector2(radius, y))
  const geometry = new THREE.LatheGeometry(points, segments)
  geometry.computeVertexNormals()
  return geometry
}

function mulberry32(seed: number) {
  let a = seed
  return () => {
    a |= 0
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

function Straw() {
  const map = useMemo(() => {
    const canvas = document.createElement('canvas')
    canvas.width = 32
    canvas.height = 160
    const ctx = canvas.getContext('2d')
    if (!ctx) return null
    ctx.fillStyle = '#121212'
    ctx.fillRect(0, 0, 32, 160)
    ctx.fillStyle = '#f4f4f4'
    ctx.fillRect(0, 96, 32, 9)
    const texture = new THREE.CanvasTexture(canvas)
    texture.colorSpace = THREE.SRGBColorSpace
    texture.wrapS = THREE.RepeatWrapping
    texture.wrapT = THREE.ClampToEdgeWrapping
    return texture
  }, [])

  return (
    <group position={[0.08, 2.48, 0.22]} rotation={[-0.72, 0.1, -0.28]}>
      <mesh position={[0, 0.78, 0]} castShadow>
        <cylinderGeometry args={[0.038, 0.038, 1.56, 24]} />
        <meshStandardMaterial map={map ?? undefined} color={map ? '#ffffff' : '#141414'} roughness={0.42} />
      </mesh>
    </group>
  )
}

function Drizzle() {
  const geometry = useMemo(() => {
    const points: THREE.Vector3[] = []
    for (let i = 0; i <= 28; i += 1) {
      const t = i / 28
      const y = 2.46 + Math.sin(t * Math.PI) * 0.46
      const angle = Math.PI / 2 + (t - 0.5) * 1.7
      const surface = radiusAt(SLUSH, Math.min(Math.max(y, 2.3), 3.02))
      const radius = surface * (0.55 + 0.28 * Math.sin(t * Math.PI))
      points.push(new THREE.Vector3(Math.cos(angle) * radius, y + 0.025, Math.sin(angle) * radius))
    }
    return new THREE.TubeGeometry(new THREE.CatmullRomCurve3(points), 80, 0.015, 8, false)
  }, [])

  return (
    <mesh geometry={geometry}>
      <meshStandardMaterial color="#fff8f2" roughness={0.3} metalness={0} />
    </mesh>
  )
}

function Crystals() {
  const ref = useRef<THREE.InstancedMesh>(null)
  const count = 86

  useLayoutEffect(() => {
    const mesh = ref.current
    if (!mesh) return
    const dummy = new THREE.Object3D()
    const color = new THREE.Color()
    const rand = mulberry32(11)
    const palette = ['#ffffff', '#fff4f7', '#ffd0dc', '#ff9bb5', '#ffe8ee']

    for (let i = 0; i < count; i += 1) {
      const y = 2.3 + Math.pow(rand(), 0.55) * 0.72
      const angle = rand() * Math.PI * 2
      const surface = Math.max(0.08, radiusAt(SLUSH, Math.min(y, 3.0)))
      const radius = surface * (0.78 + rand() * 0.32)
      dummy.position.set(Math.cos(angle) * radius, y, Math.sin(angle) * radius)
      dummy.rotation.set(rand() * 4, rand() * 4, rand() * 4)
      const scale = 0.03 + rand() * 0.055
      dummy.scale.set(scale * (0.65 + rand() * 0.7), scale * (0.55 + rand() * 0.8), scale * (0.7 + rand() * 0.6))
      dummy.updateMatrix()
      mesh.setMatrixAt(i, dummy.matrix)
      color.set(palette[Math.floor(rand() * palette.length)])
      mesh.setColorAt(i, color)
    }
    mesh.instanceMatrix.needsUpdate = true
    if (mesh.instanceColor) mesh.instanceColor.needsUpdate = true
  }, [])

  return (
    <instancedMesh ref={ref} args={[undefined, undefined, count]} castShadow>
      <icosahedronGeometry args={[1, 0]} />
      <meshPhysicalMaterial
        roughness={0.18}
        metalness={0}
        transmission={0.45}
        thickness={0.25}
        ior={1.31}
        color="#ffe4ec"
      />
    </instancedMesh>
  )
}

function Droplets() {
  const ref = useRef<THREE.InstancedMesh>(null)
  const count = 42

  useLayoutEffect(() => {
    const mesh = ref.current
    if (!mesh) return
    const dummy = new THREE.Object3D()
    const rand = mulberry32(29)

    for (let i = 0; i < count; i += 1) {
      const y = 0.35 + rand() * 1.85
      const angle = rand() * Math.PI * 2
      const radius = radiusAt(CUP, y) + 0.014
      dummy.position.set(Math.sin(angle) * radius, y, Math.cos(angle) * radius)
      dummy.rotation.set(0, 0, 0)
      const drop = rand() > 0.84
      const s = 0.01 + rand() * 0.012
      dummy.scale.set(s, drop ? s * 3.4 : s * 1.5, s)
      dummy.updateMatrix()
      mesh.setMatrixAt(i, dummy.matrix)
    }
    mesh.instanceMatrix.needsUpdate = true
  }, [])

  return (
    <instancedMesh ref={ref} args={[undefined, undefined, count]}>
      <sphereGeometry args={[1, 12, 10]} />
      <meshPhysicalMaterial
        color="#f7fbff"
        roughness={0.05}
        transmission={0.92}
        thickness={0.15}
        transparent
        opacity={0.55}
      />
    </instancedMesh>
  )
}

function LogoSticker() {
  const texture = useTexture(logoUrl)

  useLayoutEffect(() => {
    texture.colorSpace = THREE.SRGBColorSpace
    texture.anisotropy = 8
    texture.needsUpdate = true
  }, [texture])

  const centerY = 1.2
  const width = 1.12
  const height = width / LOGO_ASPECT
  const z = radiusAt(CUP, centerY) + 0.07

  return (
    <group position={[0, centerY, z]}>
      <mesh position={[0, 0, -0.006]} renderOrder={3}>
        <planeGeometry args={[width * 1.08, height * 1.16]} />
        <meshStandardMaterial color="#070707" roughness={0.6} metalness={0.04} />
      </mesh>
      <mesh position={[0, 0, 0]} renderOrder={4}>
        <planeGeometry args={[width, height]} />
        <meshBasicMaterial map={texture} transparent toneMapped={false} />
      </mesh>
    </group>
  )
}

function GranizadoModel() {
  const group = useRef<THREE.Group>(null)
  const slush = useRef<THREE.ShaderMaterial>(null)
  const cupGeometry = useMemo(() => lathe(CUP, 96), [])
  const slushGeometry = useMemo(() => lathe(SLUSH, 80), [])

  useFrame((_, delta) => {
    if (group.current) group.current.rotation.y += delta * 0.5
    if (slush.current) slush.current.uniforms.uTime.value += delta
  })

  return (
    <group ref={group}>
      <mesh geometry={cupGeometry} castShadow>
        <MeshTransmissionMaterial
          backside
          samples={6}
          resolution={512}
          thickness={0.08}
          roughness={0.015}
          anisotropy={0.04}
          chromaticAberration={0.012}
          ior={1.45}
          distortion={0.015}
          distortionScale={0.08}
          temporalDistortion={0}
          clearcoat={1}
          clearcoatRoughness={0.04}
          color="#ffffff"
          attenuationDistance={6}
          attenuationColor="#ffe4ee"
        />
      </mesh>
      <mesh geometry={slushGeometry}>
        <shaderMaterial
          ref={slush}
          side={THREE.DoubleSide}
          vertexShader={slushVertex}
          fragmentShader={slushFragment}
          uniforms={{ uTime: { value: 0 } }}
        />
      </mesh>
      <mesh position={[0, 2.4, 0]} rotation={[Math.PI / 2, 0, 0]}>
        <torusGeometry args={[0.83, 0.02, 12, 96]} />
        <meshPhysicalMaterial
          color="#ffffff"
          roughness={0.12}
          transmission={0.85}
          thickness={0.12}
          ior={1.45}
        />
      </mesh>
      <LogoSticker />
      <Crystals />
      <Droplets />
      <Drizzle />
      <Straw />
    </group>
  )
}

function FloorGlow() {
  const texture = useMemo(() => {
    const canvas = document.createElement('canvas')
    canvas.width = 512
    canvas.height = 512
    const ctx = canvas.getContext('2d')
    if (!ctx) return null
    const glow = ctx.createRadialGradient(256, 256, 16, 256, 256, 250)
    glow.addColorStop(0, 'rgba(255,255,255,0.2)')
    glow.addColorStop(0.42, 'rgba(255,255,255,0.05)')
    glow.addColorStop(1, 'rgba(255,255,255,0)')
    ctx.fillStyle = glow
    ctx.fillRect(0, 0, 512, 512)
    const map = new THREE.CanvasTexture(canvas)
    map.colorSpace = THREE.SRGBColorSpace
    return map
  }, [])

  if (!texture) return null

  return (
    <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.002, 0]}>
      <circleGeometry args={[2.6, 64]} />
      <meshBasicMaterial map={texture} transparent depthWrite={false} toneMapped={false} />
    </mesh>
  )
}

export function GranizadoScene() {
  return (
    <>
      <ambientLight intensity={0.18} />
      <directionalLight position={[3.2, 5.4, 2.4]} intensity={2.4} castShadow color="#fff7f2" />
      <directionalLight position={[-3.5, 2.4, -2.2]} intensity={1.7} color="#f2f7ff" />
      <spotLight position={[0.4, 4.8, 1.2]} angle={0.55} penumbra={0.6} intensity={8} color="#ffe4ee" />
      <Environment frames={1} resolution={256}>
        <Lightformer intensity={3.2} position={[0, 4.2, 2]} scale={[8, 3, 1]} color="#ffffff" />
        <Lightformer intensity={1.6} position={[-4, 1.5, -2]} scale={[4, 6, 1]} color="#ffc2d4" />
        <Lightformer intensity={1.8} position={[4, 2, -3]} scale={[3, 7, 1]} color="#e7f2ff" />
        <Lightformer
          intensity={0.8}
          position={[0, -1, 3]}
          rotation={[Math.PI / 2, 0, 0]}
          scale={[6, 6, 1]}
          color="#ff4d6d"
        />
      </Environment>
      <GranizadoModel />
      <FloorGlow />
      <ContactShadows position={[0, -0.001, 0]} opacity={0.55} scale={7} blur={2.4} far={3.4} color="#140006" />
      <OrbitControls
        enablePan={false}
        enableDamping
        dampingFactor={0.08}
        minDistance={4.4}
        maxDistance={9}
        minPolarAngle={0.72}
        maxPolarAngle={1.38}
        target={[0, 1.45, 0]}
      />
    </>
  )
}
