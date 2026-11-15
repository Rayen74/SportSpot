import * as THREE from 'three';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';

/**
 * SportSpot Hero 3D Renderer & Interface Controller
 * Optimized for mid-range Android devices, low GPU fill cost, zero memory leaks,
 * demand-driven render scheduling, and lifecycle safety.
 */

// Global state & configuration
const CONFIG = {
  refWidth: 390,
  refHeight: 844,
  iridescenceRange: [250, 650],
  shatterDuration: 1.2,
  isLowEndDevice: (typeof navigator !== 'undefined' && navigator.hardwareConcurrency && navigator.hardwareConcurrency <= 4),
  reducedMotion: window.matchMedia('(prefers-reduced-motion: reduce)').matches
};

// Adaptive Quality Tiers: LOW, MEDIUM, HIGH
const QUALITY = {
  tier: CONFIG.reducedMotion ? 'LOW' : (CONFIG.isLowEndDevice ? 'LOW' : 'MEDIUM'),
  maxPixelRatio: CONFIG.isLowEndDevice ? 1.25 : 1.5,
  antialias: !CONFIG.isLowEndDevice,
  bevelSegments: CONFIG.isLowEndDevice ? 1 : 2
};

// Expose globals early on window object for Chrome DevTools inspection & native Activity lifecycle control
window.CONFIG = CONFIG;
window.QUALITY = QUALITY;
window.SportSpotHero = {
  dispose: () => dispose(),
  pause: () => {
    isSceneActive = false;
    if (animFrameId) {
      cancelAnimationFrame(animFrameId);
      animFrameId = null;
    }
    isRenderLoopRunning = false;
  },
  resume: () => {
    isSceneActive = true;
    clock.getDelta();
    renderFrame();
    requestDemandFrame();
  }
};

let renderer = null;
let scene = null;
let camera = null;
let pmremGenerator = null;
let roomEnvTexture = null;
let glassMaterial = null;
let shards = [];
let shardGroup = null;

let isShattered = false;
let isReassembling = false;
let shatterProgress = 0.0;

let touchX = 0;
let touchY = 0;
let targetTiltX = 0;
let targetTiltY = 0;
let currentTiltX = 0;
let currentTiltY = 0;
let panelScale = 1.0;

let animFrameId = null;
let isRenderLoopRunning = false;
let isSceneActive = true; // false when on Journey view or backgrounded
let isPointerDown = false;

const clock = new THREE.Clock();
const raycaster = new THREE.Raycaster();
const mouseVector = new THREE.Vector2();

// Uniforms for global touch glow shader
const touchUniforms = {
  uTouchPos: { value: new THREE.Vector2(-999, -999) },
  uTouchIntensity: { value: 0.0 },
  uScale: { value: 1.0 }
};

// Polygon layout normalized to [0, 1] panel space
const CUTS = {
  T0: [0.22, 0.0],
  T1: [0.515, 0.0],
  T2: [0.84, 0.0],
  B0: [0.34, 1.0],
  B1: [0.865, 1.0],
  J0: [0.385, 0.72],
  J1: [0.48, 0.62],
  J2: [0.716, 0.49]
};

const CELLS = [
  { id: 'A', name: 'hero', pts: [[0, 0], CUTS.T0, CUTS.J0, CUTS.B0, [0, 1]] },
  { id: 'B', name: 'top-mid', pts: [CUTS.T0, CUTS.T1, CUTS.J1, CUTS.J0] },
  { id: 'C', name: 'center', pts: [CUTS.T1, CUTS.T2, CUTS.J2, CUTS.J1] },
  { id: 'D', name: 'bot-mid', pts: [CUTS.J0, CUTS.J1, CUTS.J2, CUTS.B1, CUTS.B0] },
  { id: 'E', name: 'right', pts: [CUTS.T2, [1, 0], [1, 1], CUTS.B1, CUTS.J2] }
];

// Event listener references for clean teardown
let boundPointerDown = null;
let boundPointerMove = null;
let boundPointerUp = null;
let boundResize = null;
let boundVisibility = null;

/**
 * Entry Point: Progressive UI startup
 */
function init() {
  // Step 1: Register UI controls immediately so navigation is interactive before 3D loads
  setupUIEvents();

  // Step 2: If user prefers reduced motion or WebGL is unsupported, static HTML is ready
  if (CONFIG.reducedMotion) {
    console.info('Reduced motion preference active. Using static presentation.');
    return;
  }

  // Step 3: Progressive WebGL scene construction
  const canvas = document.getElementById('webgl-canvas');
  if (!canvas) return;

  try {
    renderer = new THREE.WebGLRenderer({
      canvas,
      antialias: QUALITY.antialias,
      alpha: false,
      powerPreference: 'high-performance',
      stencil: false,
      depth: true
    });
  } catch (e) {
    console.warn('WebGL initialization failed, falling back to static UI:', e);
    showFallback();
    return;
  }

  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, QUALITY.maxPixelRatio));
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.1;

  scene = new THREE.Scene();
  scene.background = new THREE.Color(0x050507);

  camera = new THREE.PerspectiveCamera(30, window.innerWidth / window.innerHeight, 0.1, 100);
  camera.position.set(0, 0, 7.5);

  // Shared PMREM Environment Texture
  pmremGenerator = new THREE.PMREMGenerator(renderer);
  pmremGenerator.compileEquirectangularShader();
  const roomEnv = new RoomEnvironment();
  roomEnvTexture = pmremGenerator.fromScene(roomEnv).texture;
  scene.environment = roomEnvTexture;
  roomEnv.dispose();

  const keyLight = new THREE.DirectionalLight(0xdbeafe, 2.2);
  keyLight.position.set(-3, 4, 5);
  scene.add(keyLight);

  const fillLight = new THREE.DirectionalLight(0x1e1b4b, 1.2);
  fillLight.position.set(3, -2, 2);
  scene.add(fillLight);

  shardGroup = new THREE.Group();
  scene.add(shardGroup);

  buildGlassShards();
  setupTouchEvents();

  boundResize = onResize;
  window.addEventListener('resize', boundResize);

  boundVisibility = onVisibilityChange;
  document.addEventListener('visibilitychange', boundVisibility);

  // Render initial frame & start demand-driven loop
  renderFrame();
  requestDemandFrame();
}

function computeScale() {
  const w = window.innerWidth;
  panelScale = Math.min(w / CONFIG.refWidth, 1.4);
  touchUniforms.uScale.value = panelScale;
  return panelScale;
}

function buildGlassShards() {
  const S = computeScale();
  const panelW = 2.8 * S;
  const panelH = 4.8 * S;

  glassMaterial = new THREE.MeshPhysicalMaterial({
    color: 0x17171B,
    roughness: 0.36,
    metalness: 0.1,
    transmission: 0.0,
    ior: 1.5,
    reflectivity: 0.7,
    clearcoat: 0.7,
    clearcoatRoughness: 0.12,
    iridescence: 0.55,
    iridescenceIOR: 1.3,
    iridescenceThicknessRange: CONFIG.iridescenceRange,
    envMapIntensity: 1.0
  });

  glassMaterial.onBeforeCompile = (shader) => {
    shader.uniforms.uTouchPos = touchUniforms.uTouchPos;
    shader.uniforms.uTouchIntensity = touchUniforms.uTouchIntensity;
    shader.uniforms.uScale = touchUniforms.uScale;

    shader.fragmentShader = `
      uniform vec2 uTouchPos;
      uniform float uTouchIntensity;
      uniform float uScale;
    ` + shader.fragmentShader;

    shader.fragmentShader = shader.fragmentShader.replace(
      '#include <dithering_fragment>',
      `
      #include <dithering_fragment>
      float d = distance(gl_FragCoord.xy, uTouchPos);
      vec3 glowColor = vec3(0.42, 0.37, 0.62) * 0.16 * exp(-1.6 * pow(d / (300.0 * uScale), 2.0));
      gl_FragColor.rgb += glowColor * uTouchIntensity;
      `
    );
  };

  CELLS.forEach((cell, idx) => {
    const shape = new THREE.Shape();
    const pts = cell.pts.map(([nx, ny]) => [
      (nx - 0.5) * panelW,
      (0.5 - ny) * panelH
    ]);

    shape.moveTo(pts[0][0], pts[0][1]);
    for (let i = 1; i < pts.length; i++) {
      shape.lineTo(pts[i][0], pts[i][1]);
    }
    shape.closePath();

    const extrudeSettings = {
      depth: 0.08 * S,
      bevelEnabled: true,
      bevelSegments: QUALITY.bevelSegments,
      steps: 1,
      bevelSize: 0.015 * S,
      bevelThickness: 0.018 * S
    };

    const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings);
    const mesh = new THREE.Mesh(geometry, glassMaterial);

    const shatterAxis = new THREE.Vector3(
      (Math.random() - 0.5) * 2,
      (Math.random() - 0.5) * 2,
      (Math.random() - 0.5) * 2
    ).normalize();
    const shatterAngle = Math.PI * (0.8 + Math.random() * 0.8);

    mesh.userData = {
      id: cell.id,
      basePos: mesh.position.clone(),
      origQuaternion: mesh.quaternion.clone(),
      shatterPos: mesh.position.clone().add(new THREE.Vector3(
        (Math.random() - 0.5) * 6.0,
        (Math.random() - 0.5) * 6.0,
        Math.random() * 3.0 + 1.5
      )),
      shatterQuaternion: mesh.quaternion.clone().multiply(
        new THREE.Quaternion().setFromAxisAngle(shatterAxis, shatterAngle)
      ),
      animLift: 0,
      driftPhase: idx * 1.3
    };

    shardGroup.add(mesh);
    shards.push(mesh);
  });
}

/**
 * Input & Gesture Event Setup
 */
function setupTouchEvents() {
  const canvas = renderer.domElement;

  boundPointerDown = (e) => handleDown(e.clientX, e.clientY);
  boundPointerMove = (e) => handleMove(e.clientX, e.clientY);
  boundPointerUp = handleUp;

  canvas.addEventListener('pointerdown', boundPointerDown);
  window.addEventListener('pointermove', boundPointerMove);
  window.addEventListener('pointerup', boundPointerUp);
  window.addEventListener('pointercancel', boundPointerUp);
}

function handleDown(x, y) {
  isPointerDown = true;
  touchX = x;
  touchY = window.innerHeight - y;
  touchUniforms.uTouchPos.value.set(touchX, touchY);
  touchUniforms.uTouchIntensity.value = 1.0;

  checkShardHit(x, y);
  requestDemandFrame();
}

function handleMove(x, y) {
  if (!isPointerDown) return;
  const dx = (x / window.innerWidth) - 0.5;
  const dy = (y / window.innerHeight) - 0.5;

  targetTiltY = THREE.MathUtils.degToRad(dx * 7.0 - 1.2);
  targetTiltX = THREE.MathUtils.degToRad(-dy * 5.5 + 0.8);

  touchX = x;
  touchY = window.innerHeight - y;
  touchUniforms.uTouchPos.value.set(touchX, touchY);
  requestDemandFrame();
}

function handleUp() {
  isPointerDown = false;
  targetTiltX = 0;
  targetTiltY = 0;
  shards.forEach(s => {
    s.userData.animLift = 0;
  });
  requestDemandFrame();
}

function checkShardHit(x, y) {
  mouseVector.set(
    (x / window.innerWidth) * 2 - 1,
    -(y / window.innerHeight) * 2 + 1
  );

  raycaster.setFromCamera(mouseVector, camera);
  const intersects = raycaster.intersectObjects(shards);

  if (intersects.length > 0 && !isShattered) {
    const hit = intersects[0].object;
    hit.userData.animLift = 0.05 * panelScale;
  }
}

/**
 * UI Controls & View Transition Setup
 */
function setupUIEvents() {
  const btnHow = document.getElementById('btn-how-it-works');
  const btnBack = document.getElementById('btn-back');
  const viewHero = document.getElementById('view-hero');
  const viewJourney = document.getElementById('view-journey');
  const canvas = document.getElementById('webgl-canvas');

  if (btnHow) {
    btnHow.addEventListener('click', () => {
      if (isShattered) return;
      triggerShatter(true);
      setTimeout(() => {
        isSceneActive = false; // Stop RAF loop while on Journey view
        if (canvas) {
          canvas.style.opacity = '0';
          canvas.style.pointerEvents = 'none';
        }
        if (viewHero) {
          viewHero.classList.remove('active');
          viewHero.classList.add('hidden');
          viewHero.setAttribute('aria-hidden', 'true');
        }
        if (viewJourney) {
          viewJourney.classList.remove('hidden');
          viewJourney.classList.add('active');
          viewJourney.setAttribute('aria-hidden', 'false');
        }
      }, 900);
    });
  }

  if (btnBack) {
    btnBack.addEventListener('click', (e) => {
      if (!isShattered) return;
      isSceneActive = true; // Re-enable scene rendering
      if (canvas) {
        canvas.style.opacity = '1';
        canvas.style.pointerEvents = 'auto';
      }
      if (viewJourney) {
        viewJourney.classList.remove('active');
        viewJourney.classList.add('hidden');
        viewJourney.setAttribute('aria-hidden', 'true');
      }

      triggerShatter(false);

      setTimeout(() => {
        if (viewHero) {
          viewHero.classList.remove('hidden');
          viewHero.classList.add('active');
          viewHero.setAttribute('aria-hidden', 'false');
        }
      }, 250);
    });
  }

  document.getElementById('btn-fallback-how')?.addEventListener('click', () => {
    document.getElementById('view-hero')?.classList.add('hidden');
    document.getElementById('view-journey')?.classList.remove('hidden');
    document.getElementById('view-journey')?.classList.add('active');
  });
}

function triggerShatter(forward) {
  isShattered = forward;
  isReassembling = !forward;
  shatterProgress = forward ? 0.0 : 1.0;
  requestDemandFrame();
}

function updateShatter(delta) {
  const speed = delta / CONFIG.shatterDuration;
  if (isShattered && shatterProgress < 1.0) {
    shatterProgress = Math.min(1.0, shatterProgress + speed);
  } else if (isReassembling && shatterProgress > 0.0) {
    shatterProgress = Math.max(0.0, shatterProgress - speed * 1.8);
    if (shatterProgress === 0.0) isReassembling = false;
  }

  let isMoving = (isShattered && shatterProgress < 1.0) || isReassembling;

  shards.forEach((shard, idx) => {
    const delay = idx * 0.06;
    const localProg = Math.max(0, Math.min(1, (shatterProgress - delay) / (1.0 - delay)));
    const ease = localProg * (2 - localProg);

    if (ease > 0) {
      shard.position.lerpVectors(shard.userData.basePos, shard.userData.shatterPos, ease);
      shard.quaternion.slerpQuaternions(shard.userData.origQuaternion, shard.userData.shatterQuaternion, ease);
    } else {
      const drift = Math.sin(clock.getElapsedTime() * 0.4 + shard.userData.driftPhase) * 0.015;
      shard.position.x = shard.userData.basePos.x;
      shard.position.y = shard.userData.basePos.y + drift;
      shard.position.z = shard.userData.basePos.z + shard.userData.animLift;
      shard.quaternion.copy(shard.userData.origQuaternion);
    }
  });

  return isMoving;
}

/**
 * Demand-driven Render Scheduler
 */
function requestDemandFrame() {
  if (!isSceneActive || !renderer) return;
  if (!isRenderLoopRunning) {
    isRenderLoopRunning = true;
    clock.getDelta(); // Reset clock delta before starting loop
    animate();
  }
}

function renderFrame() {
  if (!renderer || !scene || !camera) return;
  renderer.render(scene, camera);
}

function animate() {
  if (!isRenderLoopRunning || !isSceneActive) {
    isRenderLoopRunning = false;
    return;
  }

  animFrameId = requestAnimationFrame(animate);

  // Clamp delta to 0.05s max to prevent large animation jumps on resume
  const delta = Math.min(clock.getDelta(), 0.05);

  // Smooth global tilt interpolation
  const tiltDiffX = targetTiltX - currentTiltX;
  const tiltDiffY = targetTiltY - currentTiltY;
  currentTiltX += tiltDiffX * 0.08;
  currentTiltY += tiltDiffY * 0.08;

  if (shardGroup) {
    shardGroup.rotation.x = currentTiltX;
    shardGroup.rotation.y = currentTiltY;
  }

  // Touch glow fade out
  if (touchUniforms.uTouchIntensity.value > 0.0) {
    touchUniforms.uTouchIntensity.value = Math.max(0, touchUniforms.uTouchIntensity.value - delta * 1.5);
  }

  const isShatterActive = updateShatter(delta);
  renderFrame();

  // Check if scene has settled into a resting static state to stop RAF loop and save CPU/GPU
  const isTilting = Math.abs(tiltDiffX) > 0.0001 || Math.abs(tiltDiffY) > 0.0001 || isPointerDown;
  const isGlowing = touchUniforms.uTouchIntensity.value > 0.001;

  if (!isShatterActive && !isTilting && !isGlowing && shatterProgress === 0.0) {
    isRenderLoopRunning = false;
    if (animFrameId) {
      cancelAnimationFrame(animFrameId);
      animFrameId = null;
    }
  }
}

function onVisibilityChange() {
  if (document.hidden) {
    isSceneActive = false;
    if (animFrameId) {
      cancelAnimationFrame(animFrameId);
      animFrameId = null;
    }
    isRenderLoopRunning = false;
  } else {
    isSceneActive = true;
    clock.getDelta(); // Reset delta on app resume
    renderFrame();
    requestDemandFrame();
  }
}

function onResize() {
  if (!renderer || !camera) return;
  const w = window.innerWidth;
  const h = window.innerHeight;
  camera.aspect = w / h;
  camera.updateProjectionMatrix();
  renderer.setSize(w, h);
  const maxPixelRatio = CONFIG.isLowEndDevice ? 1.25 : 1.5;
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, maxPixelRatio));
  computeScale();
  renderFrame();
}

function showFallback() {
  document.getElementById('webgl-canvas')?.remove();
  document.getElementById('static-fallback')?.classList.remove('hidden');
  document.getElementById('static-fallback')?.setAttribute('aria-hidden', 'false');
}

/**
 * Idempotent Resource Disposal API
 */
function dispose() {
  isSceneActive = false;
  isRenderLoopRunning = false;

  if (animFrameId) {
    cancelAnimationFrame(animFrameId);
    animFrameId = null;
  }

  if (boundResize) window.removeEventListener('resize', boundResize);
  if (boundVisibility) document.removeEventListener('visibilitychange', boundVisibility);
  if (boundPointerMove) window.removeEventListener('pointermove', boundPointerMove);
  if (boundPointerUp) {
    window.removeEventListener('pointerup', boundPointerUp);
    window.removeEventListener('pointercancel', boundPointerUp);
  }

  const canvas = renderer ? renderer.domElement : null;
  if (canvas && boundPointerDown) {
    canvas.removeEventListener('pointerdown', boundPointerDown);
  }

  shards.forEach((shard) => {
    if (shard.geometry) shard.geometry.dispose();
  });
  shards = [];

  if (glassMaterial) {
    glassMaterial.dispose();
    glassMaterial = null;
  }

  if (roomEnvTexture) {
    roomEnvTexture.dispose();
    roomEnvTexture = null;
  }

  if (pmremGenerator) {
    pmremGenerator.dispose();
    pmremGenerator = null;
  }

  if (renderer) {
    renderer.dispose();
    renderer.forceContextLoss();
    renderer = null;
  }
}

// Global hook for Activity / WebView lifecycle control
window.SportSpotHero = {
  dispose,
  pause: () => {
    isSceneActive = false;
    if (animFrameId) {
      cancelAnimationFrame(animFrameId);
      animFrameId = null;
    }
    isRenderLoopRunning = false;
  },
  resume: () => {
    isSceneActive = true;
    clock.getDelta();
    renderFrame();
    requestDemandFrame();
  }
};

window.addEventListener('DOMContentLoaded', init);
