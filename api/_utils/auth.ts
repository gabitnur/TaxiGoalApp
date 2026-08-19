import * as admin from 'firebase-admin';

if (!admin.apps.length) {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT || '{}');
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}

export async function verifyAuth(authHeader: string | undefined) {
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        throw new Error('UNAUTHENTICATED');
    }
    const token = authHeader.split('Bearer ')[1];
    try {
        const decodedToken = await admin.auth().verifyIdToken(token);
        return decodedToken;
    } catch (error) {
        console.error('Auth verification failed:', error);
        throw new Error('INVALID_AUTH');
    }
}
