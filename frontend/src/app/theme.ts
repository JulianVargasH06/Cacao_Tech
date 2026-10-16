import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

export const CacaoPreset = definePreset(Aura, {
    semantic: {
        primary: {
            50: '#faf6f2',
            100: '#f1e7dc',
            200: '#e2cdb7',
            300: '#cfae8a',
            400: '#bb8d62',
            500: '#a67348',
            600: '#8f5d39',
            700: '#754a31',
            800: '#5f3d2d',
            900: '#4f3328',
            950: '#2c1a14'
        },
        // Esquinas más redondeadas en campos de formulario y en las superficies en general
        formField: {
            borderRadius: '0.9rem'
        },
        content: {
            borderRadius: '1rem'
        }
    },
    components: {
        // Botones en forma de píldora y un poco más amplios
        button: {
            root: {
                borderRadius: '999px',
                paddingX: '1.4rem',
                paddingY: '0.6rem'
            }
        },
        // Tarjetas tipo cristal: semitransparentes, muy redondeadas y con sombra suave
        card: {
            root: {
                background: 'rgba(255, 255, 255, 0.65)',
                borderRadius: '1.5rem',
                shadow: '0 10px 30px rgba(117, 74, 49, 0.10)'
            }
        },
        drawer: {
            root: {
                background: 'rgba(255, 255, 255, 0.82)',
                borderColor: 'rgba(255, 255, 255, 0.7)'
            }
        },
        // El menú lateral se funde con el panel: sin caja propia y con opciones en píldora
        menu: {
            root: {
                background: 'transparent',
                borderColor: 'transparent'
            },
            item: {
                borderRadius: '999px'
            }
        }
    }
});