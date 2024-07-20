import { SignJWT } from 'jose'

export const SUPPORTED_ALGORITHMS = [
  'HS256', // HMAC using SHA-256
  'HS384', // HMAC using SHA-384
  'HS512' // HMAC using SHA-512
  // 'RS256', // RSASSA-PKCS1-v1_5 using SHA-256
  // 'RS384', // RSASSA-PKCS1-v1_5 using SHA-384
  // 'RS512', // RSASSA-PKCS1-v1_5 using SHA-512
  // 'PS256', // RSASSA-PSS using SHA-256
  // 'PS384', // RSASSA-PSS using SHA-384
  // 'PS512', // RSASSA-PSS using SHA-512
  // 'ES256', // ECDSA using P-256 and SHA-256
  // 'ES384', // ECDSA using P-384 and SHA-384
  // 'ES512', // ECDSA using P-521 and SHA-512
  // 'EdDSA' // EdDSA using Ed25519 or Ed448
]

export async function generateJWT (payload, secretKey, alg) {
  const secret = new TextEncoder().encode(secretKey)
  return await new SignJWT(payload)
    .setProtectedHeader({ alg, typ: 'JWT' })
    .sign(secret)
}
